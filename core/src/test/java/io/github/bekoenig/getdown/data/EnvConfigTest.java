//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2018 Getdown authors
// https://github.com/bekoenig/getdown/blob/master/LICENSE

package io.github.bekoenig.getdown.data;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnvConfigTest {

    static final String CWD = System.getProperty("user.dir");
    static final String TESTID = "testid";
    static final String TESTBASE = "https://test.com/test";

    static void checkNoNotes(List<EnvConfig.Note> notes) {
        StringBuilder msg = new StringBuilder();
        for (EnvConfig.Note note : notes) {
            if (note.level != EnvConfig.Note.Level.INFO) {
                msg.append("\n").append(note.message);
            }
        }
        if (msg.length() > 0) {
            fail("Unexpected notes:" + msg);
        }
    }

    private void checkDir(EnvConfig cfg) {
        assertNotNull(cfg);
        assertEquals(new File(CWD), cfg.appDir);
    }

    private void checkNoAppId(EnvConfig cfg) {
        assertNull(cfg.appId);
    }

    private void checkAppId(EnvConfig cfg, String appId) {
        assertEquals(appId, cfg.appId);
    }

    private void checkAppBase(EnvConfig cfg, String appBase) {
        assertEquals(appBase, cfg.appBase);
    }

    private void checkNoAppBase(EnvConfig cfg) {
        assertNull(cfg.appBase);
    }

    private void checkNoAppArgs(EnvConfig cfg) {
        assertTrue(cfg.appArgs.isEmpty());
    }

    private void checkAppArgs(EnvConfig cfg, String... args) {
        assertEquals(Arrays.asList(args), cfg.appArgs);
    }

    @Test
    void testArgvDir() {
        List<EnvConfig.Note> notes = new ArrayList<>();
        String[] args = {CWD};
        EnvConfig cfg = EnvConfig.create(args, notes);
        // debugNotes(notes);
        checkNoNotes(notes);
        checkDir(cfg);
        checkNoAppId(cfg);
        checkNoAppBase(cfg);
        checkNoAppArgs(cfg);
    }

    @Test
    void testArgvDirId() {
        List<EnvConfig.Note> notes = new ArrayList<>();
        String[] args = {CWD, TESTID};
        EnvConfig cfg = EnvConfig.create(args, notes);
        // debugNotes(notes);
        checkNoNotes(notes);
        checkDir(cfg);
        checkAppId(cfg, TESTID);
        checkNoAppBase(cfg);
        checkNoAppArgs(cfg);
    }

    @Test
    void testArgvDirArgs() {
        List<EnvConfig.Note> notes = new ArrayList<>();
        String[] args = {CWD, "", "one", "two"};
        EnvConfig cfg = EnvConfig.create(args, notes);
        // debugNotes(notes);
        checkNoNotes(notes);
        checkDir(cfg);
        checkNoAppId(cfg);
        checkNoAppBase(cfg);
        checkAppArgs(cfg, "one", "two");
    }

    @Test
    void testArgvDirIdArgs() {
        List<EnvConfig.Note> notes = new ArrayList<>();
        String[] args = {CWD, TESTID, "one", "two"};
        EnvConfig cfg = EnvConfig.create(args, notes);
        // debugNotes(notes);
        checkNoNotes(notes);
        checkDir(cfg);
        checkAppId(cfg, TESTID);
        checkNoAppBase(cfg);
        checkAppArgs(cfg, "one", "two");
    }

    private EnvConfig sysPropsConfig(List<EnvConfig.Note> notes, String... keyVals) {
        for (int ii = 0; ii < keyVals.length; ii += 2) {
            System.setProperty(keyVals[ii], keyVals[ii + 1]);
        }
        EnvConfig cfg = EnvConfig.create(new String[0], notes);
        for (int ii = 0; ii < keyVals.length; ii += 2) {
            System.clearProperty(keyVals[ii]);
        }
        return cfg;
    }

    @Test
    void testSysPropsDir() {
        List<EnvConfig.Note> notes = new ArrayList<>();
        EnvConfig cfg = sysPropsConfig(notes, "appdir", CWD);
        // debugNotes(notes);
        checkNoNotes(notes);
        checkDir(cfg);
        checkNoAppId(cfg);
        checkNoAppBase(cfg);
        checkNoAppArgs(cfg);
    }

    @Test
    void testSysPropsDirIdBase() {
        List<EnvConfig.Note> notes = new ArrayList<>();
        EnvConfig cfg = sysPropsConfig(notes, "appdir", CWD, "appid", TESTID, "appbase", TESTBASE);
        // debugNotes(notes);
        checkNoNotes(notes);
        checkDir(cfg);
        checkAppId(cfg, TESTID);
        checkAppBase(cfg, TESTBASE);
        checkNoAppArgs(cfg);
    }
}
