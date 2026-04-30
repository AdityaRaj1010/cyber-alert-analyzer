package util;

import model.SecurityAlert;

import java.util.Comparator;

/**
 * Custom {@link Comparator} for {@link SecurityAlert}.
 *
 * Demonstrates implementing the Comparator interface "by hand" instead of
 * relying on Comparator.comparing() + thenComparing() chains, so the
 * sorting logic is explicit and visible to the reader.
 *
 * Supports any {@link Field} as the sort key and any {@link Direction}
 * (ascending / descending). The direction can be flipped at runtime via
 * {@link #toggleDirection()} - used by the dashboard "↑ ASC / ↓ DESC"
 * toggle button.
 *
 * Usage examples:
 *
 *   // Sort newest first
 *   list.sort(new SecurityAlertComparator(Field.TIME, Direction.DESC));
 *
 *   // Toggle direction on the same comparator instance
 *   SecurityAlertComparator c = new SecurityAlertComparator(Field.SEVERITY, Direction.ASC);
 *   list.sort(c);
 *   c.toggleDirection();
 *   list.sort(c);
 */
public class SecurityAlertComparator implements Comparator<SecurityAlert> {

    /** Which property of {@link SecurityAlert} to sort by. */
    public enum Field { ID, TIME, SEVERITY, TYPE, USER, MESSAGE }

    /** Sort direction. */
    public enum Direction { ASC, DESC }

    private Field     field;
    private Direction direction;

    public SecurityAlertComparator() {
        this(Field.TIME, Direction.DESC);                    // newest first by default
    }

    public SecurityAlertComparator(Field field, Direction direction) {
        this.field     = field;
        this.direction = direction;
    }

    /* -------------- accessors -------------- */
    public Field     getField()     { return field; }
    public Direction getDirection() { return direction; }
    public void      setField(Field f)        { this.field = f; }
    public void      setDirection(Direction d){ this.direction = d; }

    /** Flip ASC <-> DESC. Convenient for the toggle-button on the dashboard. */
    public void toggleDirection() {
        this.direction = (this.direction == Direction.ASC) ? Direction.DESC : Direction.ASC;
    }

    /** Return a NEW comparator with the opposite direction (handy for builder-style use). */
    public SecurityAlertComparator reversed() {
        return new SecurityAlertComparator(field,
                direction == Direction.ASC ? Direction.DESC : Direction.ASC);
    }

    /* -------------- the actual comparison -------------- */
    @Override
    public int compare(SecurityAlert a, SecurityAlert b) {
        if (a == null && b == null) return 0;
        if (a == null) return direction == Direction.ASC ? -1 :  1;
        if (b == null) return direction == Direction.ASC ?  1 : -1;

        int cmp;
        switch (field) {
            case ID:
                cmp = Integer.compare(a.getAlertId(), b.getAlertId());
                break;
            case TIME:
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) cmp = 0;
                else if (a.getCreatedAt() == null) cmp = -1;
                else if (b.getCreatedAt() == null) cmp =  1;
                else cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                break;
            case SEVERITY:
                // Severity is an enum: LOW < MEDIUM < HIGH < CRITICAL.
                // ASC means LOW first, DESC means CRITICAL first.
                cmp = a.getSeverity().compareTo(b.getSeverity());
                break;
            case TYPE:
                cmp = nullSafeIgnoreCase(a.getAlertType(), b.getAlertType());
                break;
            case USER:
                cmp = nullSafeIgnoreCase(a.getUsername(),  b.getUsername());
                break;
            case MESSAGE:
                cmp = nullSafeIgnoreCase(a.getMessage(),   b.getMessage());
                break;
            default:
                cmp = 0;
        }

        // Stable tie-breaker: alert ID.
        if (cmp == 0 && field != Field.ID) {
            cmp = Integer.compare(a.getAlertId(), b.getAlertId());
        }

        return direction == Direction.ASC ? cmp : -cmp;
    }

    private static int nullSafeIgnoreCase(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return  1;
        return a.compareToIgnoreCase(b);
    }

    @Override
    public String toString() {
        return "SecurityAlertComparator[" + field + " " + direction + "]";
    }
}
