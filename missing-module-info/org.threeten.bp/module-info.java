module org.threeten.bp {
    requires transitive java.sql;

    exports org.threeten.bp;
    exports org.threeten.bp.chrono;
    exports org.threeten.bp.format;
    exports org.threeten.bp.jdk8;
    exports org.threeten.bp.temporal;
    exports org.threeten.bp.zone;

    provides org.threeten.bp.zone.ZoneRulesProvider with
        org.threeten.bp.zone.TzdbZoneRulesProvider;

}
