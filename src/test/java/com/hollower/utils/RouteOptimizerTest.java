package com.hollower.utils;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteOptimizerTest {
    private static final int BUDGET_MILLIS = 300;

    private static RouteOptimizer.Options options(
            float horizontal, float vertical, float turn, boolean closedLoop, boolean pinFirst) {
        return new RouteOptimizer.Options(horizontal, vertical, turn, closedLoop, pinFirst, BUDGET_MILLIS);
    }

    private static RouteOptimizer.Options defaults() {
        return options(1.0f, 1.0f, 0.0f, true, true);
    }

    // A 5x5 grid on one level, shuffled out of any sensible order.
    private static List<BlockPos> scrambledGrid(int size, int y) {
        List<BlockPos> nodes = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                nodes.add(new BlockPos(x * 10, y, z * 10));
            }
        }
        Collections.shuffle(nodes, new Random(7));
        return nodes;
    }

    // Counts the corners in a route that bend by more than `minDegrees`.
    private static int countBends(List<BlockPos> order, boolean closedLoop, double minDegrees) {
        int n = order.size();
        double cosLimit = Math.cos(Math.toRadians(minDegrees));
        int bends = 0;
        for (int p = 0; p < n; p++) {
            if (!closedLoop && (p == 0 || p == n - 1)) continue;
            BlockPos a = order.get((p - 1 + n) % n);
            BlockPos b = order.get(p);
            BlockPos c = order.get((p + 1) % n);

            double ux = b.getX() - a.getX(), uy = b.getY() - a.getY(), uz = b.getZ() - a.getZ();
            double vx = c.getX() - b.getX(), vy = c.getY() - b.getY(), vz = c.getZ() - b.getZ();
            double uLength = Math.sqrt(ux * ux + uy * uy + uz * uz);
            double vLength = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (uLength == 0.0 || vLength == 0.0) continue;

            double dot = (ux * vx + uy * vy + uz * vz) / (uLength * vLength);
            if (dot < cosLimit) bends++;
        }
        return bends;
    }

    // ---------------------------------------------------------------- invariants

    @Test
    @DisplayName("the reported cost matches a fresh scoring of the returned order")
    void reportedCostMatchesReturnedOrder() {
        // This is the load-bearing test: it is what catches the search reporting a cost that its
        // own output does not actually have.
        for (float turn : new float[]{0.0f, 8.0f, 40.0f}) {
            for (boolean loop : new boolean[]{true, false}) {
                for (boolean pin : new boolean[]{true, false}) {
                    RouteOptimizer.Options opts = options(1.0f, 2.0f, turn, loop, pin);
                    RouteOptimizer.Result result = RouteOptimizer.optimize(scrambledGrid(5, 64), opts);
                    assertEquals(RouteOptimizer.cost(result.order(), opts), result.costAfter(), 1.0E-6,
                            "turn=" + turn + " loop=" + loop + " pin=" + pin);
                }
            }
        }
    }

    @Test
    @DisplayName("the result is a permutation of the input")
    void resultIsAPermutation() {
        List<BlockPos> input = scrambledGrid(5, 64);
        List<BlockPos> order = RouteOptimizer.optimize(input, defaults()).order();

        assertEquals(input.size(), order.size());
        assertEquals(new HashSet<>(input), new HashSet<>(order));
    }

    @Test
    @DisplayName("optimizing never makes a route worse")
    void neverWorseThanTheInput() {
        for (boolean loop : new boolean[]{true, false}) {
            RouteOptimizer.Options opts = options(1.0f, 2.0f, 8.0f, loop, true);
            RouteOptimizer.Result result = RouteOptimizer.optimize(scrambledGrid(4, 64), opts);
            assertTrue(result.costAfter() <= result.costBefore() + 1.0E-6,
                    result.costAfter() + " > " + result.costBefore());
        }
    }

    @Test
    @DisplayName("a pinned first node stays first")
    void pinnedFirstNodeStaysFirst() {
        List<BlockPos> input = scrambledGrid(5, 64);
        for (boolean loop : new boolean[]{true, false}) {
            List<BlockPos> order =
                    RouteOptimizer.optimize(input, options(1.0f, 1.0f, 8.0f, loop, true)).order();
            assertEquals(input.getFirst(), order.getFirst(), "loop=" + loop);
        }
    }

    // ---------------------------------------------------------------- the two knobs

    @Test
    @DisplayName("a turn penalty trades sharp corners away as it rises")
    void turnPenaltyRemovesSharpCorners() {
        // The point of the penalty is to stop the route pivoting hard at every node. It does not
        // produce a serpentine sweep, and shouldn't: a run of gentle diagonal bends is cheaper under
        // this cost model than a few square corners, and is nicer to mine. So the property to hold
        // the optimizer to is that *sharp* corners go away as the weight rises, not that any
        // particular shape appears.
        List<BlockPos> input = scrambledGrid(5, 64);

        int previousSharp = Integer.MAX_VALUE;
        for (float turn : new float[]{0.0f, 20.0f, 60.0f, 200.0f}) {
            List<BlockPos> order =
                    RouteOptimizer.optimize(input, options(1.0f, 1.0f, turn, false, false)).order();
            int sharp = countBends(order, false, 80.0);
            assertTrue(sharp <= previousSharp,
                    "sharp corners rose from " + previousSharp + " to " + sharp + " at turn=" + turn);
            previousSharp = sharp;
        }

        int withoutPenalty =
                countBends(RouteOptimizer.optimize(input, options(1.0f, 1.0f, 0.0f, false, false)).order(),
                        false, 80.0);
        assertTrue(previousSharp * 2 <= withoutPenalty,
                "a heavy turn penalty should at least halve the sharp corners, got "
                        + previousSharp + " vs " + withoutPenalty);
    }

    @Test
    @DisplayName("a turn penalty buys straightness with extra distance, not by shortening the route")
    void turnPenaltyCostsRawDistance() {
        List<BlockPos> input = scrambledGrid(5, 64);
        RouteOptimizer.Options distanceOnly = options(1.0f, 1.0f, 0.0f, false, false);

        List<BlockPos> shortest = RouteOptimizer.optimize(input, distanceOnly).order();
        List<BlockPos> straightened =
                RouteOptimizer.optimize(input, options(1.0f, 1.0f, 200.0f, false, false)).order();

        // Scored purely on distance, the straightened route is the worse of the two. That is the
        // trade being made, and it is the reason the penalty is a separate knob.
        assertTrue(RouteOptimizer.cost(straightened, distanceOnly)
                        > RouteOptimizer.cost(shortest, distanceOnly),
                "expected the straightened route to be longer in raw distance");
    }

    @Test
    @DisplayName("a high vertical scale finishes one level before changing height")
    void verticalScaleGroupsByLevel() {
        List<BlockPos> input = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 3; z++) {
                input.add(new BlockPos(x * 10, 64, z * 10));
                input.add(new BlockPos(x * 10, 120, z * 10));
            }
        }
        Collections.shuffle(input, new Random(11));

        List<BlockPos> order =
                RouteOptimizer.optimize(input, options(1.0f, 20.0f, 0.0f, false, false)).order();

        int levelChanges = 0;
        for (int i = 1; i < order.size(); i++) {
            if (order.get(i).getY() != order.get(i - 1).getY()) levelChanges++;
        }
        assertEquals(1, levelChanges, "expected the two levels to be visited as two blocks");
    }

    @Test
    @DisplayName("an open path is cheaper than the same route as a loop")
    void openPathSkipsTheReturnLeg() {
        List<BlockPos> input = scrambledGrid(4, 64);
        double loop = RouteOptimizer.optimize(input, options(1.0f, 1.0f, 0.0f, true, true)).costAfter();
        double path = RouteOptimizer.optimize(input, options(1.0f, 1.0f, 0.0f, false, true)).costAfter();
        assertTrue(path < loop, path + " should be cheaper than " + loop);
    }

    // ---------------------------------------------------------------- degenerate input

    @Test
    @DisplayName("tiny and duplicate-heavy routes come back untouched rather than throwing")
    void degenerateInputIsSafe() {
        for (int n = 0; n <= 3; n++) {
            List<BlockPos> input = new ArrayList<>();
            for (int i = 0; i < n; i++) input.add(new BlockPos(i, 64, i));
            RouteOptimizer.Result result = RouteOptimizer.optimize(input, defaults());
            assertEquals(input, result.order());
            assertFalse(result.changed());
        }

        List<BlockPos> duplicates = List.of(
                new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), new BlockPos(0, 64, 0),
                new BlockPos(10, 64, 0), new BlockPos(10, 64, 0), new BlockPos(10, 64, 10));
        RouteOptimizer.Options opts = options(1.0f, 2.0f, 8.0f, true, true);
        RouteOptimizer.Result result = RouteOptimizer.optimize(duplicates, opts);
        assertEquals(duplicates.size(), result.order().size());
        assertEquals(RouteOptimizer.cost(result.order(), opts), result.costAfter(), 1.0E-6);
    }

    @Test
    @DisplayName("the same route and settings always give the same answer")
    void resultIsDeterministic() {
        List<BlockPos> input = scrambledGrid(5, 64);
        RouteOptimizer.Options opts = options(1.0f, 2.0f, 8.0f, true, true);
        assertEquals(
                RouteOptimizer.optimize(input, opts).order(),
                RouteOptimizer.optimize(input, opts).order());
    }
}
