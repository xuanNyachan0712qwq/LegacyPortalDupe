package com.example;

public interface LegacyPortalDelayFlag {

    boolean legacyPortalDupe$shouldDelayPortal();

    void legacyPortalDupe$markPortalDelayed();

    void legacyPortalDupe$resetPortalDelay();

    boolean legacyPortalDupe$wasPortalDelayed();

    boolean legacyPortalDupe$isLegacyTransferPending();

    void legacyPortalDupe$markLegacyTransferPending();

    void legacyPortalDupe$clearLegacyTransferPending();
}