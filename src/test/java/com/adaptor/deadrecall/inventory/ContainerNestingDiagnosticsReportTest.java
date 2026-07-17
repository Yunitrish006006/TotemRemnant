package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.inventory.ContainerNestingDiagnostics.Direction;
import com.adaptor.deadrecall.inventory.ContainerNestingDiagnostics.Finding;
import com.adaptor.deadrecall.inventory.ContainerNestingDiagnostics.ScanReport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerNestingDiagnosticsReportTest {
    @Test
    void mergePreservesTotalsAndMarksOmittedFindingsAsTruncated() {
        List<ScanReport> reports = List.of(report("alpha", 200), report("beta", 100));

        ScanReport merged = ContainerNestingDiagnostics.merge(reports);

        assertEquals(2, merged.scannedRoots());
        assertEquals(300, merged.scannedStacks());
        assertEquals(300, merged.totalFindings());
        assertEquals(ContainerNestingDiagnostics.MAX_RETAINED_FINDINGS, merged.findings().size());
        assertTrue(merged.truncated());
        assertFalse(merged.clean());
    }

    @Test
    void mergeOfSmallCleanReportsRemainsCompleteAndClean() {
        ScanReport merged = ContainerNestingDiagnostics.merge(List.of(
                new ScanReport("alpha", 2, 3, 0, List.of(), false),
                new ScanReport("beta", 1, 4, 0, List.of(), false)
        ));

        assertEquals(3, merged.scannedRoots());
        assertEquals(7, merged.scannedStacks());
        assertTrue(merged.clean());
        assertFalse(merged.truncated());
    }

    private static ScanReport report(String owner, int count) {
        List<Finding> findings = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            findings.add(new Finding(
                    owner,
                    "inventory[0]/container[" + index + "]",
                    "deadrecall:backpack_basic",
                    "minecraft:bundle",
                    1,
                    Direction.RESTRICTED_CONTAINER_INSIDE_BACKPACK
            ));
        }
        return new ScanReport(owner, 1, count, count, List.copyOf(findings), false);
    }
}
