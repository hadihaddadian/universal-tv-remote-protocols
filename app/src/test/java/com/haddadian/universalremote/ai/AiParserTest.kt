package com.haddadian.universalremote.ai
import org.junit.Assert.*; import org.junit.Test
class AiParserTest{
 private val p=OfflineTvCommandParser()
 @Test fun colloquialMute(){assertTrue(p.parse("خفه‌ش کن",TvContext()).intent is TvIntent.Mute)}
 @Test fun volume(){assertEquals(TvIntent.SetVolume(25),p.parse("صدا رو روی 25 بذار",TvContext()).intent)}
 @Test fun reject(){assertNull(p.parse("فردا هوا چطوره؟",TvContext()).intent)}
}
