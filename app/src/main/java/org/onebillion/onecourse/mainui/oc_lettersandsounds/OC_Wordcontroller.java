package org.onebillion.onecourse.mainui.oc_lettersandsounds;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import org.onebillion.onecourse.controls.OBLabel;
import org.onebillion.onecourse.mainui.OC_SectionController;
import org.onebillion.onecourse.utils.OBFont;
import org.onebillion.onecourse.utils.OBPhoneme;
import org.onebillion.onecourse.utils.OBSyllable;
import org.onebillion.onecourse.utils.OBUtils;
import org.onebillion.onecourse.utils.OBWord;
import org.onebillion.onecourse.utils.OBXMLManager;
import org.onebillion.onecourse.utils.OBXMLNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alan on 27/06/16.
 */
public class OC_Wordcontroller extends OC_SectionController
{
    protected boolean needDemo;

    public void start()
    {
        setStatus(0);
        OBUtils.runOnOtherThread(new OBUtils.RunLambda()
        {
            public void run() throws Exception
            {
                try
                {
                    if(!performSel("demo",currentEvent()) )
                    {
                        doBody(currentEvent());
                    }
                }
                catch(Exception exception) {
                }
            }
        });
    }

    public long switchStatus(String scene)
    {
        return setStatus(STATUS_AWAITING_CLICK);
    }

    void SetColourForLabel(OBLabel lab,int col)
    {
        lab.setColour(col);
    }
    public void highlightLabel(OBLabel lab, boolean h)
    {
        lockScreen();
        int col;
        if(h)
            col = Color.RED;
        else
            col = Color.BLACK;
        SetColourForLabel(lab, col);
        unlockScreen();
    }

    public void playLetterSound(final String s)
    {
        OBUtils.runOnMainThread(new OBUtils.RunLambda()
        {
            @Override
            public void run() throws Exception
            {
                String fn;
                if (s.startsWith("is_"))
                    fn = s;
                else
                    fn = String.format("is_%s",s);
                playAudio(fn);
            }
        });
    }

    public void playLetterName(final String s)
    {
        OBUtils.runOnMainThread(new OBUtils.RunLambda()
        {
            @Override
            public void run() throws Exception
            {
                String fn;
                if (s.startsWith("alph_"))
                    fn = s;
                else
                    fn = String.format("alph_%s",s);
                playAudio(fn);
            }
        });
    }

    public boolean itemsInSameDirectory(String item1,String item2)
    {
        String path1 = getLocalPath(String.format("%s.m4a",item1));
        if (path1 == null)
            return false;
        String p1 = OBUtils.stringByDeletingLastPathComponent(path1);
        if (p1 == null)
            return false;
        String path2 = getLocalPath(String.format("%s.m4a",item2));
        if (path2 == null)
            return false;
        String p2 = OBUtils.stringByDeletingLastPathComponent(path2);
        if (p2 == null)
            return false;
        return p1.equals(p2);
    }

    public void playFirstSoundOfWordId(String wordID,OBWord rw)
    {
        try
        {
            String fileName = wordID.replaceFirst("_","_let_") ;
            if(fileName != null && itemsInSameDirectory(fileName,wordID) )
            {
                List<List<Double>> timings = OBUtils.ComponentTimingsForWord(getLocalPath(fileName+".etpa"));
                if(timings.size()  > 0)
                {
                    List<Double> timing = timings.get(0);
                    double timeStart = timing.get(0) ;
                    double timeEnd = timing.get(1);
                    playAudioFromTo(fileName,timeStart,timeEnd);
                    return;
                }
            }
            if(rw != null)
            {
                OBPhoneme obphn = rw.syllables().get(0).phonemes.get(0);
                String s = obphn.soundid;
                if (s == null)
                    s = obphn.text;
                playLetterSound(s);
            }
        }
        catch(Exception exception)
        {
            Log.i("itemsinsame","here");
        }
    }

    public void highlightAndSpeakSyllablesForWord(final OBWord w)
    {
        highlightAndSpeakSyllablesForWord(w,false);
    }

    public void highlightAndSpeakSyllablesForWord(final OBWord w,final Boolean leaveHighlighted)
    {
        try
        {
            String wordID = w.soundid;
            String fileName = wordID.replace("fc_", "fc_syl_");
            List<List<Double>> timings = OBUtils.ComponentTimingsForWord(getLocalPath(fileName + ".etpa"));
            if (timings.size() > 0)
            {
                playAudio(fileName);
                long startTime = SystemClock.uptimeMillis();
                int i = 0;
                int rangelocation = 0,rangelength = 0;
                for (OBSyllable syllable : w.syllables())
                {
                    double currTime = (SystemClock.uptimeMillis() - startTime) / 1000.0;
                    List<Double> timing = timings.get(i);
                    double timeStart = timing.get(0);
                    double timeEnd = timing.get(1);
                    double waitTime = timeStart - currTime;
                    if (waitTime > 0.0)
                        waitForSecs(waitTime);
                    rangelength = syllable.text.length();
                    highlightWrd(w,rangelocation,rangelocation+rangelength,true);
                    currTime = (SystemClock.uptimeMillis() - startTime) / 1000.0;
                    waitTime = timeEnd - currTime;
                    if (waitTime > 0.0)
                        waitForSecs(waitTime);
                    highlightWrd(w,rangelocation,rangelocation+rangelength,false);

                    rangelocation += rangelength;
                    rangelength = 0;
                    i++;
                }
            }
            waitForSecs(0.3f);
            highlightWrd(w,0,w.text.length(),true);
            playAudioQueued(Collections.singletonList((Object)wordID),true);
            if (!leaveHighlighted)
                highlightWrd(w,0,w.text.length(),false);
            waitForSecs(0.3f);
        }
        catch (Exception exception)
        {
        }
        OBUtils.runOnMainThread(new OBUtils.RunLambda()
        {
            public void run() throws Exception
            {
                if (!leaveHighlighted)
                    highlightWrd(w,0,w.text.length(),false);
            }
        });
    }

    public void highlightAndSpeakIndividualPhonemes(List<OBLabel> labs,List<String>phonemes)
    {
        long token = -1;
        try
        {
            token = takeSequenceLockInterrupt(true);
            if(token == sequenceToken)
            {
                int ct = Math.min(labs.size() , phonemes.size() );
                for(int i = 0;i < ct;i++)
                {
                    String fileName = phonemes.get(i);
                    playAudio(fileName);
                    highlightLabel(labs.get(i),true);
                    waitAudio();
                    highlightLabel(labs.get(i),false);
                    checkSequenceToken(token);
                    waitForSecs(0.3f);
                }
            }
        }
        catch(Exception exception)
        {
        }

        lockScreen();
        for(OBLabel l : labs)
            highlightLabel(l,false);
        unlockScreen();
        sequenceLock.unlock();
    }

    public void highlightAndSpeakComponents(List<OBLabel> labs,String wordID,String s,String fileName)
    {
        long token = -1;
        try
        {
            token = takeSequenceLockInterrupt(true);
            if(token == sequenceToken)
            {
                List<List<Double>> timings = OBUtils.ComponentTimingsForWord(getLocalPath(fileName+".etpa"));
                playAudio(fileName);
                long startTime = SystemClock.uptimeMillis();
                int rangelocation = 0,rangelength = 0;
                for(int i = 0; i < labs.size();i++)
                {
                    double currTime = (SystemClock.uptimeMillis() - startTime) / 1000.0;
                    List<Double> timing = timings.get(i);
                    double timeStart = timing.get(0);
                    double timeEnd = timing.get(1);
                    double waitTime = timeStart - currTime;
                    if (waitTime > 0.0)
                        waitForSecs(waitTime);
                    checkSequenceToken(token);
                    rangelocation = i;
                    rangelength = 1;
                    highlightLabel(labs.get(i),true);
                    currTime = (SystemClock.uptimeMillis() - startTime) / 1000.0;
                    waitTime = timeEnd - currTime;
                    if(waitTime > 0.0 && token == sequenceToken)
                        waitForSecs(waitTime);
                    highlightLabel(labs.get(i),false);
                    checkSequenceToken(token);

                    rangelength = 0;
                }
                checkSequenceToken(token);
                waitForSecs(0.3f);
            }
        }
        catch(Exception exception)
        {
        }
        lockScreen();
        for(OBLabel l : labs)
            highlightLabel(l,false);
        unlockScreen();
        sequenceLock.unlock();
    }

    public void highlightWrd(OBWord w,int rangestart,int rangeend,boolean h)
    {
        OBLabel lab = (OBLabel) w.properties.get("label");
        lockScreen();
        if (h)
            lab.setHighRange(rangestart,rangeend,Color.RED);
        else
            lab.setHighRange(-1,-1,Color.BLACK);
        unlockScreen();
    }

    public Map LoadLetterXML(String xmlPath)
    {
        Map dict = new HashMap();
        if(xmlPath != null)
        {
            try
            {
                OBXMLManager xmlman = new OBXMLManager();
                List<OBXMLNode> xl = xmlman.parseFile(OBUtils.getInputStreamForPath(xmlPath));
                OBXMLNode root = xl.get(0);
                for(OBXMLNode letterNode : root.childrenOfType("letter"))
                {
                    String name = letterNode.attributeStringValue("id");
                    String tags = letterNode.attributeStringValue("tags");
                    Map lttr = new HashMap();
                    if (tags != null)
                    {
                        for(String tag : tags.split("/"))
                        {
                            List<String> pars = Arrays.asList(tag.split("="));
                            String k = pars.get(0);
                            String val;
                            if(pars.size()  < 2)
                                val = "true";
                            else
                                val = pars.get(1);
                            lttr.put(k,val);
                        }
                    }
                    dict.put(name,lttr);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return dict;
    }

    public Object findTarget(PointF pt)
    {
        return finger(-1,2,targets,pt);
    }

    public static RectF boundingBoxForText(String tx, Typeface ty, float textsize)
    {
        TextPaint tp = new TextPaint();
        tp.setTextSize(textsize);
        tp.setTypeface(ty);
        tp.setColor(Color.BLACK);
        SpannableString ss = new SpannableString(tx);
        StaticLayout sl = new StaticLayout(ss,tp,4000, Layout.Alignment.ALIGN_NORMAL,1,0,false);
        return new RectF(0,0,sl.getLineRight(0),sl.getLineBottom(0));
    }

    public static RectF boundingBoxForText(String tx, OBFont font)
    {
        return boundingBoxForText(tx,font.typeFace,font.size);
    }

}
