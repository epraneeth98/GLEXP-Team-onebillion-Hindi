package org.onebillion.xprz.mainui.generic;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.SystemClock;
import android.view.View;

import org.onebillion.xprz.controls.OBControl;
import org.onebillion.xprz.controls.OBGroup;
import org.onebillion.xprz.controls.OBImage;
import org.onebillion.xprz.controls.OBPath;
import org.onebillion.xprz.mainui.XPRZ_Tracer;
import org.onebillion.xprz.utils.OBRunnableSyncUI;
import org.onebillion.xprz.utils.OBUserPressedBackException;
import org.onebillion.xprz.utils.OBUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Created by pedroloureiro on 12/07/16.
 */
public class XPRZ_Generic_Tracing extends XPRZ_Tracer
{
    protected static float tracingSpeed = 2.0f;
    //
    protected int currentDemoAudioIndex;
    protected int currentTry;
    //
    protected OBGroup path1, path2;
    protected OBImage dash;
    protected OBControl trace_arrow;
    private Boolean autoClean;

    public XPRZ_Generic_Tracing (Boolean autoClean)
    {
        this.autoClean = autoClean;
    }


    public void pointer_demoTrace (Boolean playAudio) throws Exception
    {
        List<OBPath> paths = (List<OBPath>) (Object) path1.filterMembers("p[0-9]+", true);
        //
        PointF destination = tracing_position_arrow();
        movePointerToPoint(destination, -25, 0.6f, true);
        //
        if (playAudio)
        {
            action_playNextDemoSentence(false);
        }
        //
        for (OBPath path : paths)
        {
            pointer_traceAlongPath(path, tracingSpeed);
            //
            subPathIndex++;
            destination = tracing_position_arrow();
            if (destination != null)
            {
                movePointerToPoint(destination, -25, 0.3f, true);
            }
        }
        //
        if (path2 != null)
        {
            paths = (List<OBPath>) (Object) path2.filterMembers("p[0-9]+", true);
            //
            for (OBPath path : paths)
            {
                pointer_traceAlongPath(path, tracingSpeed);
                //
                subPathIndex++;
                destination = tracing_position_arrow();
                if (destination != null)
                {
                    movePointerToPoint(destination, -25, 0.3f, true);
                }
            }
        }
        if (playAudio)
        {
            waitAudio();
        }
    }


    public void action_answerIsCorrect () throws Exception
    {
        if (currentTry == 1)
        {
            gotItRightBigTick(false);
            waitSFX();
            waitForSecs(0.3);
            //
            playAudioQueuedScene(currentEvent(), "CORRECT", true);
            waitForSecs(0.7);
            //
            currentTry++;
            playAudioQueuedScene(currentEvent(), "PROMPT2", false);
            //
            if (autoClean)
            {
                lockScreen();
                tracing_reset();
                unlockScreen();
                //
                setStatus(STATUS_WAITING_FOR_TRACE);
            }
            else
            {
                setStatus(STATUS_AWAITING_CLICK);
            }
        }
        else
        {
            gotItRightBigTick(true);
            //
            playAudioQueuedScene(currentEvent(), "CORRECT", true);
            waitForSecs(0.3);
            //
            playAudioQueuedScene(currentEvent(), "FINAL", true);
            waitForSecs(0.3);
            //
            nextScene();
        }
    }


    public void prepare ()
    {
        super.prepare();
        loadFingers();
        loadEvent("master1");
        String scenes = (String) eventAttributes.get(action_getScenesProperty());
        if (scenes != null)
        {
            String[] eva = scenes.split(",");
            events = Arrays.asList(eva);
        }
        else
        {
            events = new ArrayList<>();
        }
        //
        if (currentEvent() != null)
        {
            doVisual(currentEvent());
        }
    }


    public void start ()
    {
        final long timeStamp = setStatus(STATUS_WAITING_FOR_TRACE);
        //
        OBUtils.runOnOtherThread(new OBUtils.RunLambda()
        {
            @Override
            public void run () throws Exception
            {
                try
                {
                    if (!performSel("demo", currentEvent()))
                    {
                        doBody(currentEvent());
                    }
                    //
                    OBUtils.runOnOtherThreadDelayed(3, new OBUtils.RunLambda()
                    {
                        @Override
                        public void run () throws Exception
                        {
                            if (!statusChanged(timeStamp))
                            {
                                playAudioQueuedScene(currentEvent(), "REMIND", false);
                            }
                        }
                    });
                }
                catch (OBUserPressedBackException e)
                {
                    stopAllAudio();
                    throw e;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }


    public void setSceneXX (String scene)
    {
        ArrayList<OBControl> oldControls = new ArrayList<>(objectDict.values());
        //
        loadEvent(scene);
        //
        Boolean redraw = eventAttributes.get("redraw") != null && eventAttributes.get("redraw").equals("true");
        if (redraw)
        {
            for (OBControl control : oldControls)
            {
                if (control == null) continue;
                detachControl(control);
                objectDict.remove(control);
            }
            //
            for (OBControl control : filterControls(".*"))
            {
                Map attributes = control.attributes();
                if (attributes != null)
                {
                    String fit = (String) attributes.get("fit");
                    if (fit != null && fit.equals("fitwidth"))
                    {
                        float scale = bounds().width() / control.width();
                        PointF position = XPRZ_Generic.copyPoint(control.position());
                        float originalHeight = control.height();
                        control.setScale(scale * control.scale());
                        float heightDiff = control.height() - originalHeight;
                        position.y += heightDiff / 2;
                        control.setPosition(position);
                    }
                }
            }
        }
        //
        currentDemoAudioIndex = 0;
        //
        XPRZ_Generic.colourObjectsWithScheme(this);
        //
        action_prepareScene(scene, redraw);
    }


    public void action_prepareScene (String scene, Boolean redraw)
    {
        currentTry = 1;
        trace_arrow = objectDict.get("trace_arrow");
        currentDemoAudioIndex = 0;
        //
        tracing_reset();
    }


    public void doAudio (String scene) throws Exception
    {
        setReplayAudioScene(currentEvent(), "REPEAT");
        playAudioQueuedScene(scene, "PROMPT", false);
    }


    public void doMainXX () throws Exception
    {
        playAudioQueuedScene(currentEvent(), "DEMO", true);
        doAudio(currentEvent());
        //
        tracing_reset();
        //
        setStatus(STATUS_WAITING_FOR_TRACE);
    }


    // TRACING


    public void tracing_reset ()
    {
        uPaths = null;
        if (subPaths != null)
        {
            for (OBControl c : subPaths)
                detachControl(c);
            subPaths = null;
        }
        //
        if (doneTraces != null)
        {
            for (OBControl c : doneTraces)
                detachControl(c);
        }
        //
        doneTraces = new ArrayList<>();
        if (currentTrace != null)
        {
            detachControl(currentTrace);
            currentTrace = null;
        }
        //
        finished = false;
        if (trace_arrow != null) trace_arrow.hide();
        //
        subPathIndex = 0;
        segmentIndex = 0;
        //
        new OBRunnableSyncUI()
        {
            public void ex ()
            {
                tracing_setup();
                tracing_position_arrow();
            }
        }.run();
    }


    public void tracing_setup ()
    {
        new OBRunnableSyncUI()
        {
            public void ex ()
            {
                pathColour = Color.BLUE;
                path1 = (OBGroup) objectDict.get("trace");
                //
                dash = (OBImage) objectDict.get("dash");
                //
                uPaths = tracing_processDigit(path1); // only one number to trace
                subPaths = new ArrayList();
                subPaths.addAll(tracing_subpathControlsFromPath("trace"));
                path1.hide();
                for (OBControl c : subPaths) c.hide();
            }
        }.run();
    }


    public List<OBPath> tracing_processDigit (OBGroup digit)
    {
        List<OBPath> arr = new ArrayList<>();
        for (int i = 1; i <= 2; i++)
        {
            OBPath p = (OBPath) digit.objectDict.get(String.format("p%d", i));
            if (p != null)
            {
                arr.add(p);
            }
            else
                break;
        }
        return arr;
    }

/*

    -(NSArray*)processDigit:(OBPath*)digit
    {
        NSMutableArray *arr = [NSMutableArray array];
        [digit setLineDashPattern:nil];
        [digit setOpacity:0.3];
        [digit setLineWidth:[self swollenLineWidth]];
        [digit sizeToBoundingBoxIncludingStroke];
        UPath *uPath = DeconstructedPath(digit.path);
        [arr addObject:uPath];
        return arr;
    }
*/


    public List<OBGroup> tracing_subpathControlsFromPath (String str)
    {
        List<OBGroup> arr = new ArrayList<>();
        OBGroup p = (OBGroup) objectDict.get(str);
        for (int i = 1; i < 10; i++)
        {
            String pp = String.format("p%d", i);
            OBPath characterfragment = (OBPath) p.objectDict.get(pp);
            if (characterfragment != null)
            {
                float savelw = characterfragment.lineWidth();
                characterfragment.setLineWidth(swollenLineWidth);
                characterfragment.sizeToBoundingBoxIncludingStroke();
                characterfragment.setLineWidth(savelw);
                characterfragment.setStrokeEnd(0.0f);
                OBGroup g = splitPath(characterfragment);
                g.setScale(p.scale());
                g.setPosition(p.position());
                g.setPosition(convertPointFromControl(characterfragment.position(), characterfragment.parent));
                attachControl(g);
                arr.add(g);
            }
            else
                break;
        }
        return arr;
    }


    public void tracing_nextSubpath ()
    {
        try
        {
            if (++subPathIndex >= subPaths.size())
            {
                setStatus(STATUS_CHECKING);
                //
                action_answerIsCorrect();
            }
            else
            {
                if (currentTrace != null)
                {
                    doneTraces.add(currentTrace);
                    currentTrace = null;
                    finished = false;
                    segmentIndex = 0;
                    positionArrow();
                }
                setStatus(STATUS_WAITING_FOR_TRACE);
            }
        }
        catch (Exception exception)
        {
        }
    }


    public void pointer_traceAlongPath (final OBPath p, float durationMultiplier) throws Exception
    {
        p.setStrokeColor(Color.BLUE);
        p.setStrokeEnd(0.0f);
        p.setOpacity(1.0f);
        //
        p.parent.primogenitor().show();
        p.show();
        trace_arrow.hide();
        //
        long starttime = SystemClock.uptimeMillis();
        float duration = p.length() * 2 * durationMultiplier / theMoveSpeed;
        float frac = 0;
        while (frac <= 1.0)
        {
            long currtime = SystemClock.uptimeMillis();
            frac = (float) (currtime - starttime) / (duration * 1000);
            final float t = (frac);
            new OBRunnableSyncUI()
            {
                public void ex ()
                {
                    p.setStrokeEnd(t);
                    thePointer.setPosition(convertPointFromControl(p.sAlongPath(t, null), p));
                    p.parent.primogenitor().setNeedsRetexture();
                }
            }.run();
            waitForSecs(0.02f);
        }
    }


    public PointF tracing_position_arrow ()
    {
        try
        {
            PointF outvec = new PointF();
            OBPath p = uPaths.get(subPathIndex);
            PointF arrowpoint = convertPointFromControl(p.sAlongPath(0.0f, outvec), p);
            trace_arrow.setPosition(arrowpoint);
            trace_arrow.rotation = (float) Math.atan2(outvec.x, -outvec.y);
            trace_arrow.show();
            trace_arrow.setZPosition(50);
            trace_arrow.setOpacity(1.0f);
            return arrowpoint;
        }
        catch (Exception e)
        {
            return null;
        }
    }


    public void action_playNextDemoSentence (Boolean waitAudio) throws Exception
    {
        playAudioQueuedSceneIndex(currentEvent(), "DEMO", currentDemoAudioIndex, waitAudio);
        currentDemoAudioIndex++;
    }


    public OBControl findTarget (PointF pt)
    {
        if (!dash.frame.contains(pt.x, pt.y))
        {
            return null;
        }
        OBControl c = finger(-1, 2, targets, pt);
        return c;
    }


    public void checkTraceStart (PointF pt)
    {
        int saveStatus = status();
        setStatus(STATUS_CHECKING);
        boolean ok = pointInSegment(pt, segmentIndex);
        if (!ok && currentTrace != null)
        {
            ok = pointInSegment(lastTracedPoint(), segmentIndex + 1) && pointInSegment(pt, segmentIndex + 1);
            if (ok)
                segmentIndex++;
        }
        if (ok)
        {
            trace_arrow.hide();
            //
            if (currentTrace == null)
            {
                startNewSubpath();
                PointF cpt = convertPointToControl(pt, currentTrace);
                currentTrace.moveToPoint(cpt.x, cpt.y);
            }
            else
            {
                PointF cpt = convertPointToControl(pt, currentTrace);
                currentTrace.addLineToPoint(cpt.x, cpt.y);
            }
            setStatus(STATUS_TRACING);
        }
        else
        {
            setStatus(saveStatus);
        }
    }


    public void touchUpAtPoint (PointF pt, View v)
    {
        if (status() == STATUS_TRACING)
        {
            setStatus(STATUS_CHECKING);
            effectMoveToPoint(pt);
            if (finished)
            {
                OBUtils.runOnOtherThread(new OBUtils.RunLambda()
                {
                    @Override
                    public void run () throws Exception
                    {
                        tracing_nextSubpath();
                    }
                });
            }
            else
            {
                setStatus(STATUS_WAITING_FOR_TRACE);
            }
        }
    }


    public void touchMovedToPoint (PointF pt, View v)
    {
        if (status() == STATUS_TRACING)
        {
            effectMoveToPoint(pt);
        }
    }


    public void touchDownAtPoint (PointF pt, View v)
    {
        if (status() == STATUS_AWAITING_CLICK)
        {
            OBControl dash = objectDict.get("dash");
            OBControl closestTarget = finger(-1, 2, filterControls("dash"), pt);
            if (closestTarget != null)
            {
                OBUtils.runOnOtherThread(new OBUtils.RunLambda()
                {
                    @Override
                    public void run () throws Exception
                    {
                        tracing_reset();
                        tracing_position_arrow();
                        setStatus(STATUS_WAITING_FOR_TRACE);
                    }
                });
            }
        }
        else if (status() == STATUS_WAITING_FOR_TRACE)
        {
            checkTraceStart(pt);
        }
    }


    public String action_getScenesProperty ()
    {
        return "scenes";
    }

}
