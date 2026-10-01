package io.github.ieswar23.strikezone.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Nine fielders (plus bowler and wicket-keeper) arranged by a captain's plan. The plan decides how
 * many boundary riders protect each scoring zone, which is what {@link ShotResolver} reads.
 */
public final class FieldSetting {

    public enum Plan {
        ATTACKING("Attacking"),
        BALANCED("Balanced"),
        DEFENSIVE("Boundary riders");

        private final String label;

        Plan(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Catalogue of positions used by the presets.
    static final Fielder SLIP = new Fielder("slip", null, false, 152, 0.20);
    static final Fielder POINT = new Fielder("point", ShotDirection.OFF, false, 86, 0.40);
    static final Fielder COVER = new Fielder("cover", ShotDirection.OFF, false, 52, 0.44);
    static final Fielder MID_OFF = new Fielder("mid-off", ShotDirection.STRAIGHT, false, 18, 0.42);
    static final Fielder MID_ON = new Fielder("mid-on", ShotDirection.STRAIGHT, false, -18, 0.42);
    static final Fielder MID_WICKET = new Fielder("mid-wicket", ShotDirection.LEG, false, -52, 0.44);
    static final Fielder SQUARE_LEG = new Fielder("square leg", ShotDirection.LEG, false, -86, 0.40);
    static final Fielder DEEP_POINT = new Fielder("deep point", ShotDirection.OFF, true, 82, 0.93);
    static final Fielder DEEP_COVER = new Fielder("deep cover", ShotDirection.OFF, true, 48, 0.94);
    static final Fielder LONG_OFF = new Fielder("long-off", ShotDirection.STRAIGHT, true, 14, 0.95);
    static final Fielder LONG_ON = new Fielder("long-on", ShotDirection.STRAIGHT, true, -14, 0.95);
    static final Fielder DEEP_MID_WICKET = new Fielder("deep mid-wicket", ShotDirection.LEG, true, -48, 0.94);
    static final Fielder DEEP_SQUARE_LEG = new Fielder("deep square leg", ShotDirection.LEG, true, -82, 0.93);
    static final Fielder THIRD_MAN = new Fielder("third man", null, true, 140, 0.92);
    static final Fielder FINE_LEG = new Fielder("fine leg", null, true, -142, 0.92);

    private final Plan plan;
    private final List<Fielder> fielders;

    public FieldSetting(Plan plan, List<Fielder> fielders) {
        this.plan = plan;
        this.fielders = Collections.unmodifiableList(new ArrayList<>(fielders));
    }

    public static FieldSetting of(Plan plan) {
        List<Fielder> list = new ArrayList<>();
        switch (plan) {
            case ATTACKING:
                Collections.addAll(list, SLIP, POINT, COVER, MID_OFF, MID_ON, MID_WICKET, SQUARE_LEG, FINE_LEG, THIRD_MAN);
                break;
            case DEFENSIVE:
                Collections.addAll(list, DEEP_POINT, DEEP_COVER, LONG_OFF, LONG_ON, DEEP_MID_WICKET, DEEP_SQUARE_LEG,
                        COVER, MID_WICKET, THIRD_MAN);
                break;
            default:
                Collections.addAll(list, POINT, COVER, MID_OFF, MID_ON, MID_WICKET, LONG_ON, DEEP_COVER,
                        DEEP_SQUARE_LEG, FINE_LEG);
                break;
        }
        return new FieldSetting(plan, list);
    }

    /**
     * Captaincy: attack in the first over, protect the boundary in the final over and mix it up in
     * between. Spinners get a balanced field more often.
     */
    public static FieldSetting forOver(int overIndex, int totalOvers, BowlerType type, Random random) {
        if (overIndex == 0 && totalOvers > 1) return of(Plan.ATTACKING);
        if (overIndex == totalOvers - 1 && totalOvers > 1) return of(Plan.DEFENSIVE);
        int roll = random.nextInt(100);
        if (type == BowlerType.SPIN) {
            return of(roll < 60 ? Plan.BALANCED : roll < 80 ? Plan.ATTACKING : Plan.DEFENSIVE);
        }
        return of(roll < 40 ? Plan.BALANCED : roll < 70 ? Plan.ATTACKING : Plan.DEFENSIVE);
    }

    public Plan getPlan() {
        return plan;
    }

    public List<Fielder> getFielders() {
        return fielders;
    }

    public boolean hasDeepFielder(ShotDirection zone) {
        return deepFielder(zone) != null;
    }

    public boolean hasInnerFielder(ShotDirection zone) {
        return innerFielder(zone) != null;
    }

    public Fielder deepFielder(ShotDirection zone) {
        for (Fielder f : fielders) {
            if (f.getZone() == zone && f.isDeep()) return f;
        }
        return null;
    }

    public Fielder innerFielder(ShotDirection zone) {
        for (Fielder f : fielders) {
            if (f.getZone() == zone && !f.isDeep()) return f;
        }
        return null;
    }

    public Fielder find(String position) {
        if (position == null) return null;
        for (Fielder f : fielders) {
            if (f.getPosition().equals(position)) return f;
        }
        return null;
    }
}
