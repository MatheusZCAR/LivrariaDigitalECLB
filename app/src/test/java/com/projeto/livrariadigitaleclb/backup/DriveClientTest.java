package com.projeto.livrariadigitaleclb.backup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DriveClientTest {

    @Test
    public void lerDataDoDrive() {
        assertEquals(1760845506123L, DriveClient.lerData("2025-10-19T03:45:06.123Z"));
        assertEquals(1760845506000L, DriveClient.lerData("2025-10-19T03:45:06Z"));
        assertEquals(0L, DriveClient.lerData(""));
        assertEquals(0L, DriveClient.lerData("ontem"));
    }
}
