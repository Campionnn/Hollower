package com.hollower.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Reorders the nodes of a route into a cheaper traversal order, using an iterated local search
// (nearest-neighbour seed, then 2-opt and Or-opt passes separated by double-bridge kicks).
//
// The cost model is deliberately not plain Euclidean distance. Two mining-specific terms matter:
//
//   * Axis scaling. X/Z and Y are weighted separately, so a vertical scale of 2 makes one block of
//     height cost as much as two blocks of horizontal travel and the tour prefers to finish a level
//     before changing height.
//   * A turn penalty. Mining a route rewards long straight runs, so each node is charged for how
//     sharply the path bends there. A tour that is slightly longer but flows in straight lines beats
//     a shorter one that zig-zags.
//
// The turn term makes the cost sequence-dependent (it depends on the predecessor, not just the
// edge), which is exactly what a classical TSP solver's (from, to) arc cost cannot express. A local
// search scores whole tours, so it gets this for free. It also stays correct under 2-opt: reversing
// a segment leaves the bend angle at every interior node unchanged.
//
// Candidate moves are scored by materialising the tour and re-costing it in full. That is O(n) per
// candidate rather than O(1), but route sizes here are tens to low hundreds of nodes, it keeps the
// scoring and the reported cost on one code path, and every search loop is bounded by a deadline.
@Environment(EnvType.CLIENT)
public final class RouteOptimizer {
    // Above this the distance matrix and the O(n^3) passes stop being reasonable. No hand-built
    // mining route comes close, so this is a guard rail rather than a real limit.
    public static final int MAX_NODES = 1024;

    private static final double EPSILON = 1.0E-9;
    // A fixed seed keeps repeated runs on the same route deterministic; a route that reshuffled
    // itself differently on every click would be worse than a marginally better tour.
    private static final long RANDOM_SEED = 20260822L;
    // How many kicks in a row may fail to improve on the best tour before the search calls it done.
    private static final int KICKS_WITHOUT_IMPROVEMENT = 40;

    public record Options(
            float horizontalScale,
            float verticalScale,
            float turnWeight,
            boolean closedLoop,
            boolean pinFirst,
            int timeBudgetMillis
    ) {
    }

    public record Result(List<BlockPos> order, double costBefore, double costAfter) {
        public boolean changed() {
            return costAfter < costBefore - EPSILON;
        }

        // How much of the original cost the reordering removed, as a percentage.
        public double improvementPercent() {
            return costBefore <= EPSILON ? 0.0 : (costBefore - costAfter) / costBefore * 100.0;
        }
    }

    private final double[] sx;
    private final double[] sy;
    private final double[] sz;
    private final double[][] dist;
    private final double turnWeight;
    private final boolean closedLoop;
    // Whether the node at index 0 is held in place during the search. For a closed loop this is
    // always true and costs nothing: every cycle can be rotated so a given node sits first, so
    // fixing index 0 rules out no solutions. For an open path it reflects the caller's pinFirst.
    private final boolean fixIndexZero;
    private final Random random = new Random(RANDOM_SEED);

    private RouteOptimizer(List<BlockPos> nodes, Options options) {
        int n = nodes.size();
        this.sx = new double[n];
        this.sy = new double[n];
        this.sz = new double[n];
        for (int i = 0; i < n; i++) {
            BlockPos pos = nodes.get(i);
            this.sx[i] = pos.getX() * options.horizontalScale();
            this.sy[i] = pos.getY() * options.verticalScale();
            this.sz[i] = pos.getZ() * options.horizontalScale();
        }

        this.dist = new double[n][n];
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                double dx = sx[a] - sx[b];
                double dy = sy[a] - sy[b];
                double dz = sz[a] - sz[b];
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dist[a][b] = d;
                dist[b][a] = d;
            }
        }

        this.turnWeight = Math.max(0.0, options.turnWeight());
        this.closedLoop = options.closedLoop();
        this.fixIndexZero = options.closedLoop() || options.pinFirst();
    }

    // Reorders `input` and reports what the reordering cost before and after. Pure: the input list is
    // copied and never mutated, so this is safe to call off the client thread.
    public static Result optimize(List<BlockPos> input, Options options) {
        List<BlockPos> nodes = List.copyOf(input);
        // Fewer than four nodes has nothing a reordering could improve on a loop, and a route that
        // large is past the point where this search is the right tool.
        if (nodes.size() < 4 || nodes.size() > MAX_NODES) {
            return new Result(nodes, 0.0, 0.0);
        }

        RouteOptimizer optimizer = new RouteOptimizer(nodes, options);
        int[] identity = new int[nodes.size()];
        for (int i = 0; i < identity.length; i++) identity[i] = i;

        double costBefore = optimizer.cost(identity);
        int[] best = optimizer.solve(identity, Math.max(1, options.timeBudgetMillis()));
        double costAfter = optimizer.cost(best);

        // Only a closed loop can be rotated freely, and only then does an unpinned first node mean
        // anything: the cost is rotation-invariant, so "unpinned" is about where the numbering
        // starts. Break the cycle at its longest hop, which is the natural place to enter the loop.
        if (options.closedLoop() && !options.pinFirst()) {
            best = optimizer.rotateToLongestEdge(best);
        }

        List<BlockPos> order = new ArrayList<>(best.length);
        for (int index : best) order.add(nodes.get(index));
        return new Result(List.copyOf(order), costBefore, costAfter);
    }

    // What a given order costs under the given settings. `optimize` reports this for the order it
    // was handed and the one it returns; exposed so callers (and tests) can score any order.
    public static double cost(List<BlockPos> order, Options options) {
        if (order.size() < 2) return 0.0;
        RouteOptimizer optimizer = new RouteOptimizer(List.copyOf(order), options);
        int[] tour = new int[order.size()];
        for (int i = 0; i < tour.length; i++) tour[i] = i;
        return optimizer.cost(tour);
    }

    // ---------------------------------------------------------------- cost

    // Cost of the bend at node `b` when arriving from `a` and leaving for `c`: 0 when the path runs
    // straight through, turnWeight/2 for a right angle, turnWeight for a full reversal. Expressed in
    // the same scaled space as the distances, so the weight reads as "blocks of detour".
    private double turnAt(int a, int b, int c) {
        if (turnWeight <= 0.0) return 0.0;

        double ux = sx[b] - sx[a];
        double uy = sy[b] - sy[a];
        double uz = sz[b] - sz[a];
        double vx = sx[c] - sx[b];
        double vy = sy[c] - sy[b];
        double vz = sz[c] - sz[b];

        double uLength = Math.sqrt(ux * ux + uy * uy + uz * uz);
        double vLength = Math.sqrt(vx * vx + vy * vy + vz * vz);
        // Duplicate nodes give no heading to compare, so they bend by nothing rather than by a
        // meaningless amount.
        if (uLength < EPSILON || vLength < EPSILON) return 0.0;

        double dot = (ux * vx + uy * vy + uz * vz) / (uLength * vLength);
        dot = Math.clamp(dot, -1.0, 1.0);
        return turnWeight * (1.0 - dot) * 0.5;
    }

    private double cost(int[] tour) {
        int n = tour.length;
        if (n < 2) return 0.0;

        double total = 0.0;
        int edges = closedLoop ? n : n - 1;
        for (int i = 0; i < edges; i++) {
            total += dist[tour[i]][tour[(i + 1) % n]];
        }

        if (turnWeight > 0.0 && n >= 3) {
            if (closedLoop) {
                for (int p = 0; p < n; p++) {
                    total += turnAt(tour[(p - 1 + n) % n], tour[p], tour[(p + 1) % n]);
                }
            } else {
                // An open path has no bend at either end; nothing arrives at the first node and
                // nothing leaves the last.
                for (int p = 1; p < n - 1; p++) {
                    total += turnAt(tour[p - 1], tour[p], tour[p + 1]);
                }
            }
        }
        return total;
    }

    // ---------------------------------------------------------------- search

    private int[] solve(int[] identity, int timeBudgetMillis) {
        long deadline = System.nanoTime() + timeBudgetMillis * 1_000_000L;

        int[] best = seed(identity);
        double bestCost = cost(best);

        int[] current = best.clone();
        double currentCost = bestCost;
        int barrenKicks = 0;
        // Converging is the normal way out; the deadline is the safety net for a route big enough
        // that the passes themselves are slow. Stopping on convergence also means a route of usual
        // size gets the same answer every run, rather than one that depends on the wall clock.
        while (barrenKicks < KICKS_WITHOUT_IMPROVEMENT && System.nanoTime() < deadline) {
            currentCost = localSearch(current, currentCost, deadline);
            if (currentCost < bestCost - EPSILON) {
                bestCost = currentCost;
                best = current.clone();
                barrenKicks = 0;
            } else {
                barrenKicks++;
            }

            // Kick out of the local optimum and descend again from the best tour so far.
            current = best.clone();
            doubleBridge(current);
            currentCost = cost(current);
        }
        return best;
    }

    // Picks the cheapest starting tour among the caller's own order and a handful of greedy ones.
    // Including the caller's order is what guarantees the result is never worse than what they had.
    private int[] seed(int[] identity) {
        int[] best = identity.clone();
        double bestCost = cost(best);

        int n = identity.length;
        // With index 0 held fixed only that one start is usable; otherwise sample a few evenly
        // spaced starts rather than all n, which would dominate the time budget on large routes.
        int starts = fixIndexZero ? 1 : Math.min(n, 8);
        for (int s = 0; s < starts; s++) {
            int[] candidate = nearestNeighbour(fixIndexZero ? 0 : s * n / starts);
            double candidateCost = cost(candidate);
            if (candidateCost < bestCost - EPSILON) {
                bestCost = candidateCost;
                best = candidate;
            }
        }
        return best;
    }

    // Greedy tour that repeatedly steps to whichever unused node is cheapest to reach, counting the
    // bend needed to get there as well as the distance.
    private int[] nearestNeighbour(int start) {
        int n = sx.length;
        boolean[] used = new boolean[n];
        int[] tour = new int[n];
        tour[0] = start;
        used[start] = true;

        for (int i = 1; i < n; i++) {
            int previous = tour[i - 1];
            int chosen = -1;
            double chosenCost = Double.MAX_VALUE;
            for (int candidate = 0; candidate < n; candidate++) {
                if (used[candidate]) continue;
                double step = dist[previous][candidate];
                if (i >= 2) step += turnAt(tour[i - 2], previous, candidate);
                if (step < chosenCost) {
                    chosenCost = step;
                    chosen = candidate;
                }
            }
            tour[i] = chosen;
            used[chosen] = true;
        }
        return tour;
    }

    // Alternates 2-opt and Or-opt passes until neither finds anything, or time runs out.
    private double localSearch(int[] tour, double currentCost, long deadline) {
        boolean improved = true;
        while (improved && System.nanoTime() < deadline) {
            improved = false;

            double after2Opt = twoOptPass(tour, currentCost, deadline);
            if (after2Opt < currentCost - EPSILON) {
                currentCost = after2Opt;
                improved = true;
            }
            if (System.nanoTime() >= deadline) break;

            double afterOrOpt = orOptPass(tour, currentCost, deadline);
            if (afterOrOpt < currentCost - EPSILON) {
                currentCost = afterOrOpt;
                improved = true;
            }
        }
        return currentCost;
    }

    // Reverses every segment in turn, keeping any reversal that helps. Untangles crossings.
    private double twoOptPass(int[] tour, double currentCost, long deadline) {
        int n = tour.length;
        int lowest = fixIndexZero ? 1 : 0;
        int[] scratch = new int[n];

        for (int i = lowest; i < n - 1; i++) {
            if (System.nanoTime() >= deadline) return currentCost;
            for (int j = i + 1; j < n; j++) {
                System.arraycopy(tour, 0, scratch, 0, n);
                reverse(scratch, i, j);
                double candidateCost = cost(scratch);
                if (candidateCost < currentCost - EPSILON) {
                    System.arraycopy(scratch, 0, tour, 0, n);
                    currentCost = candidateCost;
                }
            }
        }
        return currentCost;
    }

    // Lifts runs of one to three nodes out and reinserts them elsewhere, in either orientation.
    // This is the move that matters most for the turn penalty: it pulls the single node that juts
    // out of an otherwise straight run back into line, which no amount of 2-opt will do.
    private double orOptPass(int[] tour, double currentCost, long deadline) {
        int n = tour.length;
        int lowest = fixIndexZero ? 1 : 0;
        int[] scratch = new int[n];

        for (int length = 1; length <= 3 && length < n; length++) {
            for (int start = lowest; start + length <= n; start++) {
                if (System.nanoTime() >= deadline) return currentCost;
                int end = start + length - 1;
                // `target` is the position to insert after; -1 means the very front of the tour.
                for (int target = lowest - 1; target < n; target++) {
                    // Landing just before the run, or anywhere inside it, is a no-op.
                    if (target >= start - 1 && target <= end) continue;
                    for (int reversed = 0; reversed <= 1; reversed++) {
                        moveSegment(tour, scratch, start, end, target, reversed == 1);
                        double candidateCost = cost(scratch);
                        if (candidateCost < currentCost - EPSILON) {
                            System.arraycopy(scratch, 0, tour, 0, n);
                            currentCost = candidateCost;
                        }
                    }
                }
            }
        }
        return currentCost;
    }

    // Writes `source` into `destination` with the run at [start, end] moved to sit just after
    // position `target` (or at the front when `target` is negative), optionally reversed.
    private static void moveSegment(
            int[] source, int[] destination, int start, int end, int target, boolean reversed) {
        int written = 0;
        if (target < 0) written = writeSegment(source, destination, written, start, end, reversed);
        for (int p = 0; p < source.length; p++) {
            if (p >= start && p <= end) continue;
            destination[written++] = source[p];
            if (p == target) written = writeSegment(source, destination, written, start, end, reversed);
        }
    }

    private static int writeSegment(
            int[] source, int[] destination, int written, int start, int end, boolean reversed) {
        for (int k = 0; k <= end - start; k++) {
            destination[written++] = reversed ? source[end - k] : source[start + k];
        }
        return written;
    }

    // The classic four-opt "double bridge": swaps two non-adjacent chunks. Local search can't undo
    // it in one move, which is what makes it a useful way out of a local optimum. Index 0 stays put.
    private void doubleBridge(int[] tour) {
        int n = tour.length;
        if (n < 8) {
            // Too short to cut into four meaningful pieces; a random swap is enough of a shake.
            int lowest = fixIndexZero ? 1 : 0;
            if (n - lowest < 2) return;
            int a = lowest + random.nextInt(n - lowest);
            int b = lowest + random.nextInt(n - lowest);
            int held = tour[a];
            tour[a] = tour[b];
            tour[b] = held;
            return;
        }

        int[] cuts = new int[3];
        for (int i = 0; i < 3; i++) cuts[i] = 1 + random.nextInt(n - 1);
        java.util.Arrays.sort(cuts);

        int[] rebuilt = new int[n];
        int written = 0;
        // A, then C, then B, then D.
        for (int p = 0; p < cuts[0]; p++) rebuilt[written++] = tour[p];
        for (int p = cuts[1]; p < cuts[2]; p++) rebuilt[written++] = tour[p];
        for (int p = cuts[0]; p < cuts[1]; p++) rebuilt[written++] = tour[p];
        for (int p = cuts[2]; p < n; p++) rebuilt[written++] = tour[p];
        System.arraycopy(rebuilt, 0, tour, 0, n);
    }

    private static void reverse(int[] tour, int from, int to) {
        while (from < to) {
            int held = tour[from];
            tour[from] = tour[to];
            tour[to] = held;
            from++;
            to--;
        }
    }

    // Rotates a cycle so it starts just after its longest hop.
    private int[] rotateToLongestEdge(int[] tour) {
        int n = tour.length;
        int cut = 0;
        double longest = -1.0;
        for (int i = 0; i < n; i++) {
            double d = dist[tour[i]][tour[(i + 1) % n]];
            if (d > longest) {
                longest = d;
                cut = (i + 1) % n;
            }
        }

        int[] rotated = new int[n];
        for (int i = 0; i < n; i++) rotated[i] = tour[(cut + i) % n];
        return rotated;
    }
}
