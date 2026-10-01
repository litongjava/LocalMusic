package com.litongjava.localmusic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class LyricsTest {
    @Test public void timestampsOffsetsRepeatedLinesAndSeeking(){
        Lyrics lyrics=Lyrics.parse("[ar:Artist]\n[offset:-500]\n[00:12.25][00:20.125]Chorus\n[00:01.5]Start\n[00:07]Middle");
        assertTrue(lyrics.timed);assertEquals(4,lyrics.lines.size());assertEquals(1000,lyrics.lines.get(0).time);
        assertEquals(11750,lyrics.lines.get(2).time);assertEquals(19625,lyrics.lines.get(3).time);
        assertEquals(-1,lyrics.active(999));assertEquals(0,lyrics.active(1000));assertEquals(2,lyrics.active(15000));assertEquals(1,lyrics.active(7000));
    }
    @Test public void plainLyricsAndBomRemainReadable(){
        Lyrics lyrics=Lyrics.parse("\uFEFF[ti:Song]\nHello\n世界");assertFalse(lyrics.timed);assertEquals(2,lyrics.lines.size());assertEquals("世界",lyrics.lines.get(1).text);
        assertTrue(Lyrics.parse("").lines.isEmpty());assertEquals(-1,lyrics.active(3000));
    }
    @Test public void readsEmbeddedUtf8UsltAndRejectsOversizedTag()throws Exception{
        ByteArrayOutputStream body=new ByteArrayOutputStream();body.write(new byte[]{3,'e','n','g',0});body.write("[00:01]Embedded lyrics".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream frames=new ByteArrayOutputStream();DataOutputStream frame=new DataOutputStream(frames);frame.writeBytes("USLT");frame.writeInt(body.size());frame.writeShort(0);frame.write(body.toByteArray());
        ByteArrayOutputStream tag=new ByteArrayOutputStream();tag.write(new byte[]{'I','D','3',3,0,0,0,0,0,(byte)frames.size()});tag.write(frames.toByteArray());
        assertEquals("[00:01]Embedded lyrics",LocalLyrics.embedded(new ByteArrayInputStream(tag.toByteArray())));
        assertEquals("",LocalLyrics.embedded(new ByteArrayInputStream(new byte[]{'I','D','3',3,0,0,127,127,127,127})));
    }
}
