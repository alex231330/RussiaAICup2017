import model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class MyStrategy implements Strategy {
    /**
     * Список целей для каждого типа техники, упорядоченных по убыванию урона по ним.
     */

    private static final Map<VehicleType, VehicleType[]> preferredTargetTypesByVehicleType;
    final VehicleType tank = VehicleType.TANK, helicopter = VehicleType.HELICOPTER, jet = VehicleType.FIGHTER, btr = VehicleType.IFV, mech = VehicleType.ARRV;

    static {
        preferredTargetTypesByVehicleType = new EnumMap<>(VehicleType.class);

        preferredTargetTypesByVehicleType.put(VehicleType.FIGHTER, new VehicleType[]{
                VehicleType.HELICOPTER, VehicleType.FIGHTER
        });

        preferredTargetTypesByVehicleType.put(VehicleType.HELICOPTER, new VehicleType[]{
                VehicleType.TANK, VehicleType.ARRV, VehicleType.HELICOPTER, VehicleType.IFV, VehicleType.FIGHTER
        });

        preferredTargetTypesByVehicleType.put(VehicleType.IFV, new VehicleType[]{
                VehicleType.HELICOPTER, VehicleType.ARRV, VehicleType.IFV, VehicleType.FIGHTER, VehicleType.TANK
        });

        preferredTargetTypesByVehicleType.put(VehicleType.TANK, new VehicleType[]{
                VehicleType.IFV, VehicleType.ARRV, VehicleType.TANK, VehicleType.FIGHTER, VehicleType.HELICOPTER
        });
    }


    private Player me;
    private World world;
    private Move move;

    private final Map<Long, Vehicle> vehicleById = new HashMap<>();
    private final Map<Long, Integer> updateTickByVehicleId = new HashMap<>();
    private final Queue<Consumer<Move>> delayedMoves = new ArrayDeque<>();

    boolean status = false;

    final double a = 1.05;
    final double b = 0.5;

    void roundAll() {
        double x = streamVehicles(Ownership.ALLY, mech).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y = streamVehicles(Ownership.ALLY, mech).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

        for (VehicleType vehicleType : VehicleType.values()) {
            delayedMoves.add(move -> {
                move.setAction(ActionType.CLEAR_AND_SELECT);
                move.setRight(world.getWidth());
                move.setBottom(world.getHeight());
                move.setVehicleType(vehicleType);
            });

            delayedMoves.add(move -> {
                move.setAction(ActionType.ROTATE);
                move.setX(x);
                move.setY(y);
                move.setAngle(Math.toRadians(180));
                System.out.println("Start moving!");
            });
        }
    }


    void scale(VehicleType v, double fac, double x, double y) {
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            delayedMoves.add(move -> {
                move.setAction(ActionType.CLEAR_AND_SELECT);
                // max X min Y
                move.setRight(world.getHeight());
                move.setBottom(world.getWidth());
                move.setVehicleType(v);
            });

            delayedMoves.add(move -> {
                move.setAction(ActionType.SCALE);
                move.setX(x);
                move.setY(y);
                move.setFactor(fac);
            });
        }
    }

    void connectUnit(VehicleType v1, VehicleType v2) {
        double x1 = streamVehicles(Ownership.ALLY, v1).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y1 = streamVehicles(Ownership.ALLY, v1).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
        double x2 = streamVehicles(Ownership.ALLY, v2).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y2 = streamVehicles(Ownership.ALLY, v2).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

        System.out.println(x1 + "  " + y1 + "  " + x2 + "  " + y2);

        if (!Double.isNaN(x1) && !Double.isNaN(y1) && !Double.isNaN(x2) && !Double.isNaN(y2)) {
            delayedMoves.add(move -> {
                move.setAction(ActionType.CLEAR_AND_SELECT);
                move.setRight(world.getWidth());
                move.setBottom(world.getHeight());
                move.setVehicleType(v1);
            });

            delayedMoves.add(move -> {
                move.setAction(ActionType.MOVE);
                move.setX(x2 - x1);
                move.setY(y2 - y1);
            });
        }
    }

    void uMove2(VehicleType v1, double x, double y) {
        double x1 = streamVehicles(Ownership.ALLY, v1).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y1 = streamVehicles(Ownership.ALLY, v1).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

        if (!Double.isNaN(x1) && !Double.isNaN(y1)) {
            delayedMoves.add(move -> {
                move.setAction(ActionType.CLEAR_AND_SELECT);
                move.setRight(world.getWidth());
                move.setBottom(world.getHeight());
                move.setVehicleType(v1);
            });
            delayedMoves.add(move -> {
                move.setAction(ActionType.MOVE);
                move.setX(x);
                move.setY(y);
            });
        }
    }


    void attack() {
        roundAll();
        double x1 = streamVehicles(Ownership.ALLY, jet).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y1 = streamVehicles(Ownership.ALLY, jet).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
        double x2 = streamVehicles(Ownership.ALLY, helicopter).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y2 = streamVehicles(Ownership.ALLY, helicopter).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
        double x3 = streamVehicles(Ownership.ALLY, tank).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y3 = streamVehicles(Ownership.ALLY, tank).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
        double x4 = streamVehicles(Ownership.ALLY, btr).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y4 = streamVehicles(Ownership.ALLY, btr).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
        double x5 = streamVehicles(Ownership.ALLY, mech).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y5 = streamVehicles(Ownership.ALLY, mech).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

        if (world.getTickIndex() % 300 == 0) {
            roundAll();
            for (int i = 0; i < 3; i++) {
                scale(jet, a, x1, y1);
                scale(helicopter, a, x2, y2);
                scale(tank, a, x3, y3);
                scale(btr, a, x4, y4);
                scale(mech, a, x5, y5);
            }
            roundAll();
            for (int i = 0; i < 3; i++) {
                scale(jet, b, x5, y5);
                scale(helicopter, b, x5, y5);
                scale(tank, b, x5, y5);
                scale(btr, b, x5, y5);
                scale(mech, b, x5, y5);
            }
        }
        if (!Double.isNaN(x5)) {
            connectUnit(jet, mech);
            connectUnit(tank, mech);
            connectUnit(helicopter, mech);
            connectUnit(btr, mech);
        } else if (Double.isNaN(x4)) {
            connectUnit(tank, jet);
            connectUnit(helicopter, jet);
            connectUnit(btr, jet);
        } else if (Double.isNaN(x3)) {
            connectUnit(helicopter, jet);
            connectUnit(btr, jet);
        } else if (Double.isNaN(x2)) {
            connectUnit(jet, jet);
        }

    }


    @Override
    public void move(Player me, World world, Game game, Move move) {
        initializeTick(me, world, game, move);

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }

        if (executeDelayedMove()) {
            return;
        }
        if (!status) {
            double x1 = streamVehicles(Ownership.ALLY, jet).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
            double y1 = streamVehicles(Ownership.ALLY, jet).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
            double x2 = streamVehicles(Ownership.ALLY, helicopter).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
            double y2 = streamVehicles(Ownership.ALLY, helicopter).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
            double x3 = streamVehicles(Ownership.ALLY, tank).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
            double y3 = streamVehicles(Ownership.ALLY, tank).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
            double x4 = streamVehicles(Ownership.ALLY, btr).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
            double y4 = streamVehicles(Ownership.ALLY, btr).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
            double x5 = streamVehicles(Ownership.ALLY, mech).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
            double y5 = streamVehicles(Ownership.ALLY, mech).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

            uMove2(tank, 5, 5);
            for (int i = 0; i < 10; i++) {
                scale(jet, a, x1, y1);
                scale(helicopter, a, x2, y2);
                scale(tank, a, x3, y3);
                scale(btr, a, x4, y4);
                scale(mech, a, x5, y5);
            }
            roundAll();
            connectUnit(jet, mech);
            connectUnit(tank, mech);
            connectUnit(helicopter, mech);
            connectUnit(btr, mech);
            for (int i = 0; i < 20; i++) {
                scale(jet, b, x5, y5);
                scale(helicopter, b, x5, y5);
                scale(tank, b, x5, y5);
                scale(btr, b, x5, y5);
                scale(mech, b, x5, y5);
            }
            roundAll();
            status = true;
        }
        attack();
        executeDelayedMove();
    }

    private void initializeTick(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        this.move = move;

        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicleById.put(vehicle.getId(), vehicle);
            updateTickByVehicleId.put(vehicle.getId(), world.getTickIndex());
        }

        for (VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {
            long vehicleId = vehicleUpdate.getId();

            if (vehicleUpdate.getDurability() == 0) {
                vehicleById.remove(vehicleId);
                updateTickByVehicleId.remove(vehicleId);
            } else {
                vehicleById.put(vehicleId, new Vehicle(vehicleById.get(vehicleId), vehicleUpdate));
                updateTickByVehicleId.put(vehicleId, world.getTickIndex());
            }
        }
    }

    private boolean executeDelayedMove() {
        if (world.getTickIndex() % 5 == 0) {
            Consumer<Move> delayedMove = delayedMoves.poll();
            if (delayedMove == null) {
                return false;
            }

            delayedMove.accept(move);
        }
        return true;
    }


    private Stream<Vehicle> streamVehicles(Ownership ownership, VehicleType vehicleType) {
        Stream<Vehicle> stream = vehicleById.values().stream();
        switch (ownership) {
            case ALLY:
                stream = stream.filter(vehicle -> vehicle.getPlayerId() == me.getId());
                break;
            case ENEMY:
                stream = stream.filter(vehicle -> vehicle.getPlayerId() != me.getId());
                break;
            default:
        }

        if (vehicleType != null) {
            stream = stream.filter(vehicle -> vehicle.getType() == vehicleType);
        }

        return stream;
    }

    private enum Ownership {
        ANY,
        ALLY,
        ENEMY
    }
}