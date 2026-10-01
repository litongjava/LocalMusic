package com.litongjava.localmusic;

import java.util.*;
import java.util.regex.*;

/** Local LRC and plain-text lyrics. No network lookup. */
public final class Lyrics {
    public static final class Line {
        public final long time;
        public final String text;
        Line(long time, String text) { this.time=time; this.text=text; }
    }
    public final List<Line> lines = new ArrayList<>();
    public boolean timed;
    public static Lyrics parse(String text) {
        Lyrics result=new Lyrics();
        Pattern stamp=Pattern.compile("\\[(\\d{1,4}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
        Matcher offset=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE).matcher(text);
        long shift=0;
        if(offset.find())try{shift=Long.parseLong(offset.group(1));}catch(NumberFormatException ignored){}
        List<Line> plain=new ArrayList<>();
        for(String raw:text.replace("\uFEFF","").split("\\r?\\n")){
            if(result.lines.size()>=2000||plain.size()>=2000)break;
            Matcher m=stamp.matcher(raw); List<Long> times=new ArrayList<>(); int end=0;
            while(m.find()){
                int seconds=Integer.parseInt(m.group(2));if(seconds>=60)continue;
                String fraction=m.group(3); long ms=fraction==null?0:Integer.parseInt((fraction+"000").substring(0,3));
                times.add(Math.max(0,Integer.parseInt(m.group(1))*60000L+seconds*1000L+ms+Math.max(-86400000,Math.min(86400000,shift))));end=m.end();
            }
            String words=raw.substring(end).trim();
            if(!times.isEmpty()){for(long time:times)result.lines.add(new Line(time,words));}
            else if(!words.isEmpty()&&!words.matches("\\[[a-zA-Z]+:.*\\]"))plain.add(new Line(-1,words));
        }
        result.timed=!result.lines.isEmpty();
        if(result.timed)Collections.sort(result.lines,(a,b)->Long.compare(a.time,b.time));else result.lines.addAll(plain);
        return result;
    }
    public int active(long position){
        if(!timed)return -1;int low=0,high=lines.size()-1,result=-1;
        while(low<=high){int mid=(low+high)>>>1;if(lines.get(mid).time<=position){result=mid;low=mid+1;}else high=mid-1;}return result;
    }
}
