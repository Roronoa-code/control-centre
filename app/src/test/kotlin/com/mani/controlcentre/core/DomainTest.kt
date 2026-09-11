package com.mani.controlcentre.core

import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    @Test fun straightSideOpensEveryday() { val r = GestureRouter(Route.SIDE); r.move(-70f, 3f); assertEquals(Page.EVERYDAY, r.finish()) }
    @Test fun sideUpOpensTools() { val r = GestureRouter(Route.SIDE); r.move(-60f, -65f); assertEquals(Page.TOOLS, r.finish()) }
    @Test fun sideDownOpensDevice() { val r = GestureRouter(Route.SIDE); r.move(-60f, 65f); assertEquals(Page.DEVICE, r.finish()) }
    @Test fun leftHandMirrorsInwardOnly() { val r = GestureRouter(Route.SIDE, true); r.move(60f, -65f); assertEquals(Page.TOOLS, r.finish()) }
    @Test fun outwardDoesNotOpen() { val r = GestureRouter(Route.SIDE); r.move(90f, 0f); assertNull(r.finish()) }
    @Test fun shortAndStationaryDoNotOpen() { val r = GestureRouter(Route.SIDE); assertNull(r.finish()); r.move(-8f, 0f); assertNull(r.finish()) }
    @Test fun returningToOriginCancelsCommit() { val r = GestureRouter(Route.SIDE); r.move(-70f, 0f); r.move(-4f, 0f); assertNull(r.finish()) }
    @Test fun cancellationIsTerminal() { val r = GestureRouter(Route.TOP); r.move(0f, 100f); r.cancel(); r.move(0f, 100f); assertNull(r.finish()) }
    @Test fun topOrdinaryPullAlwaysEveryday() { val r = GestureRouter(Route.TOP); r.move(-20f, 90f); assertEquals(Page.EVERYDAY, r.finish()) }
    @Test fun topRailSelectsToolsDuringSamePull() { val r = GestureRouter(Route.TOP); r.move(-90f, 90f); assertEquals(Page.TOOLS, r.finish()) }
    @Test fun topRailSelectsDeviceDuringSamePull() { val r = GestureRouter(Route.TOP); r.move(-160f, 90f); assertEquals(Page.DEVICE, r.finish()) }
    @Test fun horizontalTopDragDoesNotOpen() { val r = GestureRouter(Route.TOP); r.move(-190f, 8f); assertNull(r.finish()) }
    @Test fun externalNeverIntercepts() { val r = GestureRouter(Route.EXTERNAL); r.move(-200f, 200f); assertNull(r.finish()) }
    @Test fun nonFiniteInputIsIgnored() { val r = GestureRouter(Route.TOP); r.move(Float.NaN, 70f); assertNull(r.finish()) }
    @Test fun routingIsAllowlisted() { assertEquals(Page.EVERYDAY, Page.parse("shell")); assertEquals(Page.TOOLS, Page.parse("tools")) }
    @Test fun layoutRoundTrips() { assertEquals(LayoutCodec.defaults(), LayoutCodec.decode(LayoutCodec.encode(LayoutCodec.defaults()))) }
    @Test fun layoutMigratesUnknownAndMissingEntries() { val result = LayoutCodec.decode("WIFI,tools,99,true;unknown,device,1,false;WIFI,device,1,false"); assertEquals(ControlId.entries.size, result.size); assertEquals(Placement(ControlId.WIFI, Page.TOOLS, 2, true), result.first()) }
    @Test fun emptyLayoutRestoresDefaults() { assertEquals(LayoutCodec.defaults(), LayoutCodec.decode("")) }
    @Test fun countCannotOverflowOrGoNegative() { assertEquals(Long.MAX_VALUE, CounterMath.increment(Long.MAX_VALUE)); assertEquals(0L, CounterMath.decrement(0L)) }
    @Test fun ownedTimeoutCanBeRestored() { assertEquals(RestoreDecision.RESTORE, restoreDecision(30000, 600000, 600000)) }
    @Test fun manualTimeoutChangeIsPreserved() { assertEquals(RestoreDecision.KEEP_USER_CHANGE, restoreDecision(30000, 600000, 120000)) }
    @Test fun missingJournalDoesNothing() { assertEquals(RestoreDecision.NOTHING, restoreDecision(null, null, 30000)) }
    @Test fun breathingUsesDeterministicPhases() { assertEquals("Breathe in", breathPhase(0).name); assertEquals("Hold", breathPhase(4000).name); assertEquals("Breathe out", breathPhase(6000).name); assertEquals(1L, breathPhase(12000).cycle) }
    @Test fun breathingProgressIsBounded() { for (ms in -100L..25000L step 53) assertTrue(breathPhase(ms).progress in 0f..1f) }
    @Test fun hostnamesAreNormalised() { assertEquals("dns.google", dnsHostname(" DNS.GOOGLE. ")); assertEquals("one.one.one.one", dnsHostname("one.one.one.one")) }
    @Test fun commandsUrlsAndIpLiteralsAreRejected() { for (s in listOf("https://dns.google", "dns.google; reboot", "1.1.1.1", "foo..com", "-dns.com", "dns-.com", "dns.com/x", "localhost", "x\n.com", "a|b.com", "dns.com:853")) assertNull(s, dnsHostname(s)) }
}
