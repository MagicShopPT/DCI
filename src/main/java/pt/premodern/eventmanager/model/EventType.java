package pt.premodern.eventmanager.model;

public enum EventType {
    SWISS_ONLY,
    SWISS_WITH_TOP_CUT;

    @Override
    public String toString() {
        return switch (this) {
            case SWISS_ONLY -> "Swiss only";
            case SWISS_WITH_TOP_CUT -> "Swiss with Top Cut";
        };
    }
}
