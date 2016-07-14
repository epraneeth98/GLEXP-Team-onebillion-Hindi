package org.onebillion.xprz.mainui.generic;

import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import org.onebillion.xprz.controls.OBControl;
import org.onebillion.xprz.controls.OBGroup;
import org.onebillion.xprz.controls.OBLabel;
import org.onebillion.xprz.controls.OBPath;
import org.onebillion.xprz.controls.OBTextLayer;
import org.onebillion.xprz.mainui.MainActivity;
import org.onebillion.xprz.mainui.XPRZ_SectionController;
import org.onebillion.xprz.utils.OBAnim;
import org.onebillion.xprz.utils.OBAnimationGroup;
import org.onebillion.xprz.utils.OBUserPressedBackException;
import org.onebillion.xprz.utils.OBUtils;
import org.onebillion.xprz.utils.OBXMLManager;
import org.onebillion.xprz.utils.OBXMLNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pedroloureiro on 20/06/16.
 */
public class XPRZ_Generic_Event extends XPRZ_SectionController
{
    public static float FIRST_REMINDER_DELAY = 6.0f;
    public static float SECOND_REMINDER_DELAY = 4.0f;
    int currentDemoAudioIndex;

    public XPRZ_Generic_Event ()
    {
        super();
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

    public void doAudio (String scene) throws Exception
    {
        setReplayAudioScene(currentEvent(), "REPEAT");
        playAudioQueuedScene(scene, "PROMPT", false);
    }


    public void doReminder () throws Exception
    {
        long stTime = statusTime;
        waitForSecs(FIRST_REMINDER_DELAY);
        doReminderWithStatusTime(stTime, true);
    }


    public void doReminderWithStatusTime (final long stTime, Boolean playAudio) throws Exception
    {
        if (statusChanged(stTime)) return;
        //
        List reminderAudio = (List<String>) ((Map<String, Object>) audioScenes.get(currentEvent())).get("REMINDER");
        if (reminderAudio != null)
        {
            if (playAudio)
            {
                playAudioQueued(reminderAudio, true);
            }
            OBUtils.runOnOtherThreadDelayed(SECOND_REMINDER_DELAY, new OBUtils.RunLambda()
            {
                @Override
                public void run () throws Exception
                {
                    doReminderWithStatusTime(stTime, false);
                }
            });
        }
    }


    public void doMainXX () throws Exception
    {
        playAudioQueuedScene(currentEvent(), "DEMO", true);
        //
        doAudio(currentEvent());
        //
        setStatus(STATUS_AWAITING_CLICK);
    }


    @Override
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
        targets = filterControls(action_getObjectPrefix() + ".*");
        //
        for (OBControl control : targets)
        {
            PointF originalPosition = new PointF(control.position().x, control.position().y);
            control.setProperty("originalPosition", originalPosition);
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
        for (OBControl control : filterControls(".*"))
        {
            control.setProperty("originalPosition", XPRZ_Generic.copyPoint(control.position()));
            //
            if (OBPath.class.isInstance(control))
            {
                OBPath path = (OBPath) control;
                PointF position = path.getWorldPosition();
                path.sizeToBoundingBoxIncludingStroke();
                path.setPosition(position);
            }
        }
    }


    public void action_playNextDemoSentence (Boolean waitAudio) throws Exception
    {
        playAudioQueuedSceneIndex(currentEvent(), "DEMO", currentDemoAudioIndex, waitAudio);
        currentDemoAudioIndex++;
        waitForSecs(0.01);
    }


    public void action_moveObjectToOriginalPosition (OBControl control, Boolean wait)
    {
        OBAnim anim = OBAnim.moveAnim((PointF) control.propertyValue("originalPosition"), control);
        OBAnimationGroup.runAnims(Arrays.asList(anim), 0.3, wait, OBAnim.ANIM_EASE_IN_EASE_OUT, this);
    }


    public void action_moveObjectIntoContainer (OBControl control, OBControl container)
    {
        List<OBControl> contained = (List<OBControl>) container.propertyValue("contained");
        if (contained == null) contained = new ArrayList<OBControl>();
        //
        if (contained.contains(control))
        {
            action_moveObjectToOriginalPosition(control, false);
        }
        else
        {
            contained.add(control);
            container.setProperty("contained", contained);
            //
            List<OBAnim> animations = new ArrayList<OBAnim>();
            // horizontal displacement
            float deltaH = container.width() / (contained.size() + 1);
            for (int i = 0; i < contained.size(); i++)
            {
                PointF newPosition = XPRZ_Generic.copyPoint(container.position());
                newPosition.x = container.left() + deltaH * (i + 1);
                OBControl placedObject = contained.get(i);
                animations.add(OBAnim.moveAnim(newPosition, placedObject));
            }
            OBAnimationGroup.runAnims(animations, 0.15, true, OBAnim.ANIM_EASE_IN_EASE_OUT, this);
        }
    }


    public void action_animatePlatform (OBControl platform, boolean wait) throws Exception
    {
        String platformName = (String) platform.attributes().get("id");
        String platformNumber = platformName.split("_")[1];
        List<OBControl> controls = filterControls(".*_" + platformNumber + "_.*");
        //
        List<OBAnim> list_animMove1 = new ArrayList<OBAnim>();
        List<OBAnim> list_animMove2 = new ArrayList<OBAnim>();
        //
        for (OBControl item : controls)
        {
            PointF startPosition = new PointF();
            startPosition.set(item.position());
            //
            PointF endPosition = new PointF();
            endPosition.set(startPosition);
            endPosition.y -= 1.25 * item.height();
            //
            list_animMove1.add(OBAnim.moveAnim(endPosition, item));
            list_animMove1.add(OBAnim.rotationAnim((float) Math.toRadians(-180.0f), item));
            list_animMove2.add(OBAnim.moveAnim(startPosition, item));
            list_animMove1.add(OBAnim.rotationAnim((float) Math.toRadians(-360.0f), item));
//            OBAnim anim_move1 = OBAnim.moveAnim(endPosition, item);
//            OBAnim anim_move2 = OBAnim.moveAnim(startPosition, item);
//            OBAnim anim_rotate1 = OBAnim.rotationAnim((float) Math.toRadians(-180.0f), item);
//            OBAnim anim_rotate2 = OBAnim.rotationAnim((float) Math.toRadians(-360.0f), item);
//            OBAnimationGroup.chainAnimations(Arrays.asList(Arrays.asList(anim_move1,anim_rotate1),Arrays.asList(anim_move2,anim_rotate2)), Arrays.asList(0.4f,0.4f), false, Arrays.asList(OBAnim.ANIM_EASE_IN, OBAnim.ANIM_EASE_OUT), 1, this);
//            waitForSecs(0.05);
        }
        OBAnimationGroup og = new OBAnimationGroup();
        og.chainAnimations(Arrays.asList(list_animMove1, list_animMove2), Arrays.asList(0.4f, 0.4f), wait, Arrays.asList(OBAnim.ANIM_EASE_IN, OBAnim.ANIM_EASE_OUT), 1, this);
    }


    public OBControl action_getCorrectAnswer ()
    {
        String correctString = action_getObjectPrefix() + "_" + eventAttributes.get("correctAnswer");
        return objectDict.get(correctString);
    }


    public String action_getObjectPrefix ()
    {
        return "number";
    }


    public String action_getContainerPrefix ()
    {
        return "box";
    }


    public String action_getScenesProperty ()
    {
        return "scenes";
    }




    public OBLabel action_createLabelForControl (OBControl control)
    {
        return action_createLabelForControl(control, 1.0f, false);
    }


    public OBLabel action_createLabelForControl (OBControl control, float finalResizeFactor)
    {
        return action_createLabelForControl(control, finalResizeFactor, true);
    }



    public OBLabel action_createLabelForControl (OBControl control, float finalResizeFactor, Boolean insertIntoGroup)
    {
        try
        {
            Boolean autoResize = eventAttributes.get("textSize") == null;
            float textSize = 1;
            //
            if (!autoResize)
            {
                textSize = applyGraphicScale(Float.parseFloat(eventAttributes.get("textSize")));
            }
            String content = (String) control.attributes().get("text");
            if (content == null) content = (String) control.attributes().get("number");
            if (content == null) content = "";
            //
            Typeface tf = OBUtils.standardTypeFace();
            OBLabel label = new OBLabel(content, tf, textSize);
            //
            if (autoResize)
            {
                OBTextLayer textLayer = (OBTextLayer) label.layer;
                textLayer.sizeToBoundingBox();
                while (label.height() > 0 && label.height() < control.bounds.height())
                {
                    textLayer.setTextSize(textLayer.textSize() + 1);
                    textLayer.sizeToBoundingBox();
                }
                //
                textLayer.setTextSize(textLayer.textSize() * finalResizeFactor);
                textLayer.sizeToBoundingBox();
            }
            //
            label.setPosition(control.position());
            label.setZPosition(XPRZ_Generic.getNextZPosition(this));
            label.texturise(false, this);
            //
            if (insertIntoGroup)
            {
                if (OBGroup.class.isInstance(control))
                {
                    OBGroup group = (OBGroup) control;
                    attachControl(label);
                    group.insertMember(label, 0, "label");
                }
                else
                {
                    OBGroup group = new OBGroup(Arrays.asList(control, label));
                    attachControl(group);
                    group.objectDict.put("frame", control);
                    group.objectDict.put("label", label);
                    String controlID = (String) control.attributes().get("id");
                    objectDict.put(controlID, group);
                    String components[] = controlID.split("_");
                    String labelID = "label_" + components[1];
                    objectDict.put(labelID, label);
                }
            }
            else
            {
                attachControl(label);
            }
            return label;
        }
        catch (Exception e)
        {
            System.out.println("XPRZ_Generic_Event:action_createLabelForControl:exception" + e.toString());
        }
        return null;
    }


    // Finger and Touch functions

    public OBControl findTarget (PointF pt)
    {
        return finger(-1, 2, targets, pt, true);
    }


    public Boolean audioSceneExists (String audioScene)
    {
        if (audioScenes == null) return false;
        //
        Map<String, List<String>> sc = (Map<String, List<String>>) audioScenes.get(currentEvent());
        //
        if (sc == null) return false;
        //
        List<Object> arr = (List<Object>) (Object) sc.get(audioScene);
        return arr != null;
    }


    // Miscelaneous Functions
    public void playSceneAudio (String scene, Boolean wait) throws Exception
    {
        playAudioQueuedScene(currentEvent(), scene, wait);
        if (!wait) waitForSecs(0.01);
    }

    public void playSceneAudioIndex (String scene, int index, Boolean wait) throws Exception
    {
        playAudioQueuedSceneIndex(currentEvent(), scene, index, wait);
        if (!wait) waitForSecs(0.01);
    }

}
