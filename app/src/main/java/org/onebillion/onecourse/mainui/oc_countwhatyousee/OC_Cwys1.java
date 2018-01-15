package org.onebillion.onecourse.mainui.oc_countwhatyousee;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.ArrayMap;
import android.view.View;

import org.onebillion.onecourse.controls.OBControl;
import org.onebillion.onecourse.controls.OBGroup;
import org.onebillion.onecourse.controls.OBLabel;
import org.onebillion.onecourse.controls.OBPath;
import org.onebillion.onecourse.mainui.OBSectionController;
import org.onebillion.onecourse.mainui.OC_SectionController;
import org.onebillion.onecourse.utils.OBAnim;
import org.onebillion.onecourse.utils.OBAnimationGroup;
import org.onebillion.onecourse.utils.OBFont;
import org.onebillion.onecourse.utils.OBMisc;
import org.onebillion.onecourse.utils.OBUtils;
import org.onebillion.onecourse.utils.OB_Maths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Created by michal on 15/01/2018.
 */

public class OC_Cwys1 extends OC_Cwys
{
    List<OBControl> currentTargets;
    List<List<Integer>> eventNumbers;
    Map<String,Integer> eventColours;
    String currentMode;
    int correctNum;
    OBLabel correctLabel;
    boolean phase2Only;
    boolean phase1;

    public String sectionAudioName()
    {

        return String.format("cwys1%s",(String)parameters.get("mode"));
    }
    public void prepare()
    {
        super.prepare();
        events = new ArrayList<>();
        currentMode = parameters.get("mode");
        boolean demo = OBUtils.getBooleanValue(parameters.get("demo"));
        eventNumbers = new ArrayList<>();
        loadEvent(String.format("cwys1%s",currentMode));
        eventColours = OBMisc.loadEventColours(this);
        phase1 = true;
        if(demo)
        {
            eventNumbers.add(Arrays.asList(0));
            events.add("demo");
        }

        if(parameters.get("phase1") != null)
        {
            String[] arr = parameters.get("phase1").split(";");
            for(int i=0; i<arr.length; i++)
            {
                eventNumbers.add(OBMisc.stringToIntegerList(arr[i],","));
                events.add(eventNameForPrefix("p1_",i+1,i==arr.length-1));
            }

        }

        if(parameters.get("phase2") != null)
        {
            String[] arr2 = parameters.get("phase2").split(";");
            for(int i=0; i<arr2.length; i++)
            {
                eventNumbers.add(OBMisc.stringToIntegerList(arr2[i],","));
                events.add(eventNameForPrefix("p2_",i+1,i==arr2.length-1));
            }
        }
        ((OBPath)objectDict.get("line")).sizeToBoundingBoxIncludingStroke();
        OBPath unit = (OBPath)objectDict.get("unit");
        unit.sizeToBoundingBoxInset(-unit.lineWidth()/2.0f);
        phase2Only = !demo && parameters.get("phase1") == null;
        if(!demo)
        {
            objectDict.get("bottom_bar").show();
            setSceneXX(currentEvent());
        }

    }

    public void start()
    {
        OBUtils.runOnOtherThread(new OBUtils.RunLambda()
        {
            public void run() throws Exception
            {
                doMainXX();
            }
        });
    }

    public void setSceneXX(String scene)
    {
        if(scene.equals("p2_1"))
        {
            phase1 = false;
        }
        currentTargets = new ArrayList<>();
        OBFont font = OBUtils.StandardReadingFontOfSize(70);
        correctNum = eventNumbers.get(eventIndex).get(0).intValue();
        List<Integer> targNums = eventNumbers.get(eventIndex);
        targNums = OBUtils.randomlySortedArray(targNums);
        for(int i=0; i<targNums.size(); i++)
        {
            int num = targNums.get(i).intValue();
            OBLabel label = new OBLabel(String.format("%d",num),font);
            label.setColour(eventColours.get("num"));
            label.setPosition(OB_Maths.locationForRect(0.3f + i*0.2f, 0.5f, objectDict.get("bottom_bar").frame()));
            label.setZPosition(10);
            if(num==correctNum)
                correctLabel = label;
            label.setProperty("start_loc",OBMisc.copyPoint(label.position()));
            attachControl(label);
            label.setLeft(label.left() + bounds().width());
            currentTargets.add(label);
        }
        loadUnitsForNumber(correctNum);
    }

    public void doMainXX() throws Exception
    {
        if(!performSel("demo_",String.format("%s%s",currentEvent() ,currentMode)))
            startScene(true);
    }

    public void touchDownAtPoint(final PointF pt, View v)
    {
        if (status() == STATUS_WAITING_FOR_DRAG)
        {
            final OBControl curTarget = finger(0, 1, currentTargets, pt);
            if (curTarget != null)
            {
                setStatus(STATUS_BUSY);
                final OC_SectionController controller = this;
                OBUtils.runOnOtherThread(new OBUtils.RunLambda()
                {
                    public void run() throws Exception
                    {
                        OBMisc.prepareForDragging(curTarget, pt, controller);
                        setStatus(STATUS_DRAGGING);
                    }
                });
            }
        }
    }

    public void touchMovedToPoint(PointF pt, View v)
    {
        if (status() == STATUS_DRAGGING && target != null)
            target.setPosition(OB_Maths.AddPoints(pt, dragOffset));
    }

    public void touchUpAtPoint(PointF pt, View v)
    {
        if (status() == STATUS_DRAGGING && target != null)
        {
            setStatus(STATUS_BUSY);
            final OBLabel curTarget = (OBLabel) target;
            target = null;
            OBUtils.runOnOtherThread(new OBUtils.RunLambda()
            {
                public void run() throws Exception
                {
                    checkTargetDrop(curTarget);
                }
            });
        }
    }

    public void startScene(boolean showUnits) throws Exception
    {
        if(showUnits)
        {
            showAllObjects();
        }
        OBMisc.doSceneAudio(4,setStatus(STATUS_WAITING_FOR_DRAG),this);
    }

    public void animateUnitsOn_a() throws Exception
    {
        lockScreen();
        for(OBGroup group : currentBlocks)
            showUnitsForBlock(group);
        playSfxAudio("squareson",false);
        unlockScreen();
        waitSFX();
    }

    public void animateUnitsOn_b() throws Exception
    {
        List<OBGroup> hundredBlocks = getCurrentHundredBlocks();
        List<OBGroup> tenColumns = getCurrentTenColumns();
        List<OBControl> units = getCurrentUnits();
        for(OBGroup block : hundredBlocks)
        {
            lockScreen();
            showUnitsForBlock(block);
            playSfxAudio("hundred",false);
            unlockScreen();
            waitSFX();
        }

        if(hundredBlocks.size() != 0 && tenColumns.size() != 0)
            waitForSecs(0.5f);

        for(OBGroup column : tenColumns)
        {
            lockScreen();
            showUnitsForColumn(column);
            playSfxAudio("ten",false);
            unlockScreen();
            waitSFX();
        }

        if(tenColumns.size() != 0 && units.size() != 0)
            waitForSecs(0.5f);

        for(OBControl unit : units)
        {
            lockScreen();
            unit.show();
            playSfxAudio("unit",false);
            unlockScreen();
            waitSFX();
        }
    }

    public void animateUnitsOn_c() throws Exception
    {
        animateUnitsOn_b();
    }

    public void slideNumbersIn() throws Exception
    {
        List<OBAnim> anims = new ArrayList<>();
        for(OBControl con : currentTargets)
            anims.add(OBAnim.moveAnim((PointF)con.propertyValue("start_loc") ,con));
        playSfxAudio("slide",false);
        OBAnimationGroup.runAnims(anims,0.5,true,OBAnim.ANIM_EASE_IN_EASE_OUT,this);
        waitSFX();
    }

    public void checkTargetDrop(OBLabel targ) throws Exception
    {
        playAudio(null);
        OBControl line = objectDict.get("line");
        boolean correct = targ == correctLabel;
        RectF hotRect = new RectF(line.left(), line.top()-targ.height(),line.left()+line.width(), line.top());
        boolean landed = hotRect.contains(targ.position().x, targ.position().y) || targ.intersectsWith(line);
        if(landed && correct)
        {
            OBAnimationGroup.runAnims(Arrays.asList(OBAnim.propertyAnim("bottom",line.position().y+targ.height()*0.25f,targ),
                    OBAnim.propertyAnim("left",line.position().x - targ.width()*1.35f*0.5f,targ),
                    OBAnim.scaleAnim(1.35f,targ),
                    OBAnim.colourAnim("colour", Color.BLACK ,targ)),0.2f,true,OBAnim.ANIM_EASE_IN_EASE_OUT,this);

            objectDict.get("line").hide();
            displayTick();
            lockScreen();
            for(OBControl con : currentTargets)
                if(con != targ)
                    ((OBLabel)con).setColour(eventColours.get("lowlight"));

            unlockScreen();
            waitForSecs(0.3f);
            boolean demoDone = performSel("countUnits_",currentMode);
            clearScene();
            nextScene();
        }
        else
        {
            if (landed)
                gotItWrongWithSfx();
            OBAnimationGroup.runAnims(Arrays.asList(OBAnim.moveAnim((PointF) targ.propertyValue("start_loc"), targ)), 0.3, true, OBAnim.ANIM_EASE_IN_EASE_OUT, this);
            setStatus(STATUS_WAITING_FOR_DRAG);
        }
    }

    public void clearScene() throws Exception
    {
        if(eventIndex == events.size()-1)
            return;
        lockScreen();
        if(currentTargets != null)
            for(OBControl con : currentTargets)
                detachControl(con);

        if(currentBlocks != null)
            for(OBControl con : currentBlocks)
                detachControl(con);
        currentTargets = null;
        currentBlocks = null;
        playSfxAudio("alloff",false);
        unlockScreen();
        waitSFX();
    }

    public void showLine() throws Exception
    {
        float width = 0;
        for(OBControl con : currentTargets)
            if(con.width() > width)
                width = con.width();

        OBControl line = objectDict.get("line");
        line.setWidth(width*1.05f*1.35f);
        line.show();
        playSfxAudio("lineon",true);
    }

    public void showBottomBar() throws Exception
    {
        objectDict.get("bottom_bar").show();
        playSfxAudio("panel",true);
    }

    public void lowLightCurrentBlocks()
    {
        lockScreen();
        for(OBGroup block : currentBlocks)
            highlightBlock(block,false);
        unlockScreen();
    }

    public List<OBGroup> getCurrentHundredBlocks()
    {
        List<OBGroup> hundredBlocks = new ArrayList<>();
        long blockCount = correctNum%100 == 0 ? currentBlocks.size() : currentBlocks.size() -1;
        for(int i=0; i<blockCount; i++)
        {
            hundredBlocks.add(currentBlocks.get(i));
        }
        return hundredBlocks;
    }

    public List<OBGroup> getCurrentTenColumns()
    {
        List<OBGroup> tenColumns = new ArrayList<>();
        OBGroup lastBlock = currentBlocks.get(currentBlocks.size()-1);
        if(correctNum%100 == 0)
            return tenColumns;

        long columnCount = correctNum%10 == 0 ? lastBlock.objectDict.size() : lastBlock.objectDict.size()-1;
        for(int i=0; i<columnCount; i++)
        {
            OBGroup column =(OBGroup)lastBlock.objectDict.get(String.format("column_%d",i+1));
            tenColumns.add(column);
        }
        return tenColumns;
    }

    public List<OBControl> getCurrentUnits()
    {
        List<OBControl> units = new ArrayList<>();
        if(correctNum%10 == 0)
            return units;
        OBGroup lastBlock = currentBlocks.get(currentBlocks.size()-1);
        long columnCount = lastBlock.objectDict.size();
        OBGroup lastColumn =(OBGroup)lastBlock.objectDict.get(String.format("column_%d",columnCount));
        for(int i=0; i<10; i++)
        {
            OBControl unit = lastColumn.objectDict.get(String.format("unit_%d",i+1));
            if(unit.isEnabled())
            {
                units.add(unit);
            }
            else
            {
                break;
            }
        }
        return units;
    }


    public void higlightAndSayCorrectLabel() throws Exception
    {
        if(correctNum == 0)
            return;
        correctLabel.setColour(Color.RED);
        playAudio(String.format("n_%d",correctNum));
        waitAudio();
        waitForSecs(0.3f);
        correctLabel.setColour(Color.BLACK);
        waitForSecs(0.3f);
    }

    public void countUnits_a() throws Exception
    {
        higlightAndSayCorrectLabel();
        for(int i=0; i<currentBlocks.size(); i++)
        {
            OBGroup block = currentBlocks.get(i);
            for(int j=0; j<block.objectDict.size(); j++)
            {
                OBGroup column =(OBGroup)block.objectDict.get(String.format("column_%d",j+1));
                for(int k=0; k<column.objectDict.size(); k++)
                {
                    int num = 100*i + 10*j + k+1;
                    OBControl unit = column.objectDict.get(String.format("unit_%d",k+1));
                    if(unit.isEnabled())
                    {
                        highlightUnit(unit,true);
                        playAudio(String.format("n_%d",num));
                        waitAudio();
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
        waitForSecs(0.3f);
        lowLightCurrentBlocks();
        waitForSecs(1.5f);
    }

    public void countUnits_b() throws Exception
    {
        if(phase1)
        {
            waitForSecs(0.5f);
            for(int i=0; i<currentBlocks.size(); i++)
            {
                OBGroup block = currentBlocks.get(i);
                for(int j=0; j<block.objectDict.size(); j++)
                {
                    int num = 100*i + 10*(j+1);
                    OBGroup column =(OBGroup)block.objectDict.get(String.format("column_%d",j+1));
                    lockScreen();
                    highlightColumn(column,true);
                    unlockScreen();
                    playAudio(String.format("n_%d",num));
                    waitAudio();
                }
            }
            waitForSecs(0.3f);
            lowLightCurrentBlocks();
            waitForSecs(0.5f);
        }
        countUnitsInGroups();
        waitForSecs(1f);
    }

    public void countUnits_c() throws Exception
    {
        if(phase1)
        {
            waitForSecs(0.5f);
            for(int i=0; i<currentBlocks.size(); i++)
            {
                int num = 100*(i+1);
                lockScreen();
                highlightBlock(currentBlocks.get(i),true);
                unlockScreen();
                playAudio(String.format("n_%d",num));
                waitAudio();
            }
            waitForSecs(0.3f);
            lowLightCurrentBlocks();
            waitForSecs(0.5f);
        }
        countUnitsInGroups();
        waitForSecs(1.5f);
    }

    public void countUnitsInGroups() throws Exception
    {
        higlightAndSayCorrectLabel();
        List<OBGroup> hundredBlocks = getCurrentHundredBlocks();
        List<OBGroup> tenColumns = getCurrentTenColumns();
        List<OBControl> units = getCurrentUnits();
        if(hundredBlocks.size() != 0)
        {
            lockScreen();
            correctLabel.setHighRange(0,1,Color.RED);
            for(OBGroup block : hundredBlocks)
                highlightBlock(block,true);

            unlockScreen();
            playAudio(String.format("plc100_%d",hundredBlocks.size()));
            waitAudio();
            waitForSecs(0.3f);
            lockScreen();
            correctLabel.setHighRange(0,1,Color.BLACK);
            for(OBGroup block : hundredBlocks)
                highlightBlock(block,false);
            unlockScreen();
            waitForSecs(0.3f);
        }

        int hiIndex = correctLabel.text().length() > 2 ? 1 : 0;
        if(tenColumns.size() != 0)
        {
            lockScreen();
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.RED);
            for(OBGroup column : tenColumns)
                highlightColumn(column,true);
            unlockScreen();
            playAudio(String.format("plc10_%d",tenColumns.size()));
            waitAudio();
            waitForSecs(0.3f);
            lockScreen();
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.BLACK);
            for(OBGroup column : tenColumns)
                highlightColumn(column,false);
            unlockScreen();
            waitForSecs(0.3f);
        }
        else if(hundredBlocks.size() != 0)
        {
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.RED);
            playAudio("plc10_0");
            waitAudio();
            waitForSecs(0.3f);
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.BLACK);
            waitForSecs(0.3f);
        }
        hiIndex = correctLabel.text().length()-1;
        if(units.size() != 0)
        {
            lockScreen();
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.RED);
            for(OBControl unit : units)
                highlightUnit(unit,true);

            unlockScreen();
            playAudio(String.format("plc1_%d",units.size()));
            waitAudio();
            waitForSecs(0.3f);
            lockScreen();
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.BLACK);
            for(OBControl unit : units)
                highlightUnit(unit,false);
            unlockScreen();
            waitForSecs(0.3f);
        }
        else
        {
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.RED);
            playAudio("plc1_0");
            waitAudio();
            waitForSecs(0.3f);
            correctLabel.setHighRange(hiIndex,hiIndex+1,Color.BLACK);
            waitForSecs(0.3f);
        }
    }

    public void showAllObjects() throws Exception
    {
        waitForSecs(0.3f);
        performSel("animateUnitsOn_",currentMode);
        waitForSecs(0.3f);
        slideNumbersIn();
        waitForSecs(0.3f);
        showLine();
        waitForSecs(0.3f);

    }

    public void demo_demob() throws Exception
    {
        waitForSecs(0.3f);
        showBottomBar();
        waitForSecs(0.3f);
        lockScreen();
        loadUnitsForNumber(10);
        unlockScreen();
        lockScreen();
        showUnitsForBlock(currentBlocks.get(0));
        playSfxAudio("ten",false);
        unlockScreen();
        waitSFX();
        waitForSecs(0.3f);
        loadPointer(POINTER_LEFT);
        moveScenePointer(OB_Maths.locationForRect(1.1f,1.15f,currentBlocks.get(0).frame()),-30,0.5f,"DEMO",0,0.3f);
        thePointer.hide();
        waitForSecs(0.3f);
        countUnits_a();
        clearScene();
        waitForSecs(0.3f);
        nextScene();
    }

    public void demo_democ() throws Exception
    {
        waitForSecs(0.3f);
        showBottomBar();
        waitForSecs(0.3f);
        OBGroup block = createBlockForUnit((OBPath)objectDict.get("unit"),100,true);
        currentBlocks  = new ArrayList<>();
        currentBlocks.add(block);
        lockScreen();
        showUnitsForBlock(block);
        PointF loc = OBMisc.copyPoint(block.position());
        loc.x = OB_Maths.locationForRect(0.5f,0.5f,this.bounds()).x;
        block.setPosition(loc);
        playSfxAudio("hundred",false);
        unlockScreen();
        waitSFX();
        waitForSecs(0.3f);
        loadPointer(POINTER_LEFT);
        moveScenePointer(OB_Maths.locationForRect(1.1f,1.15f,currentBlocks.get(0).frame()),-30,0.5f,"DEMO",0,0.3f);
        lowLightCurrentBlocks();
        waitForSecs(0.3f);
        List<OBAnim> anims = new ArrayList<>();
        float centerX = block.bounds.width()/2.0f;
        OBPath unit  = (OBPath)objectDict.get("unit");
        float unitWidth = unit.width() - unit.lineWidth();
        for(int i=0; i<10; i++)
        {
            OBGroup column =(OBGroup)block.objectDict.get(String.format("column_%d",i+1));
            lockScreen();
            highlightColumn(column,true);
            unlockScreen();
            playAudio(String.format("n_%d",(i+1) *10));
            waitAudio();
            waitForSecs(0.2f);
            anims.add(OBAnim.propertyAnim("left",centerX +((i-5) *unitWidth) ,column));
        }
        waitForSecs(0.3f);
        lowLightCurrentBlocks();
        waitForSecs(0.3f);
        moveScenePointer(OB_Maths.locationForRect(0.6f,1.2f,currentBlocks.get(0).frame()),-30,0.5f,"DEMO",1,0.3f);
        playSfxAudio("makehundred",false);
        OBAnimationGroup.runAnims(anims,0.5,true,OBAnim.ANIM_EASE_IN_EASE_OUT,this);
        waitSFX();
        moveScenePointer(OB_Maths.locationForRect(0.8f,1.2f,currentBlocks.get(0).frame()),-30,0.5f,"DEMO",2,0.3f);
        thePointer.hide();
        waitForSecs(1.5f);
        clearScene();
        waitForSecs(0.3f);
        nextScene();

    }

    public void demo_p1_1a() throws Exception
    {
        demo_p1_1(currentBlocks.get(0),false,true,"DEMO");

    }

    public void demo_p1_1b() throws Exception
    {
        List<OBGroup> arr = getCurrentTenColumns();
        demo_p1_1(arr.get(0),true,false,"DEMO");
    }

    public void demo_p1_1c() throws Exception
    {
        List<OBGroup> arr = getCurrentHundredBlocks();
        demo_p1_1(arr.get(0),false,false,"DEMO");
    }

    public void demo_p1_1(OBGroup pointGroup,boolean isColumn,boolean modeA,String audioCategory) throws Exception
    {
        showAllObjects();
        loadPointer(POINTER_LEFT);
        int index = 0;
        movePointerToPoint(OB_Maths.locationForRect(0.5f,1.15f,pointGroup.getWorldFrame()),-30,0.5f,true);
        if(!modeA)
        {
            lockScreen();
            if(isColumn)
                highlightColumn(pointGroup,true);
            else
                highlightBlock(pointGroup,true);

            unlockScreen();
        }
        playAudioScene(audioCategory,0,true);
        waitForSecs(0.3f);
        if(!modeA)
        {
            lockScreen();
            if(isColumn)
                highlightColumn(pointGroup,false);
            else
                highlightBlock(pointGroup,false);
            unlockScreen();
            index++;
            moveScenePointer(OB_Maths.locationForRect(1.1f,1.15f,currentBlocks.get(currentBlocks.size()-1).getWorldFrame()),-20,0.5f,audioCategory,index,0.3f);
        }
        movePointerToPoint(OB_Maths.locationForRect(0.2f,0.9f,objectDict.get("bottom_bar") .frame()),-40,0.5f,true);
        index++;
        playAudioScene(audioCategory,index,false);
        movePointerToPoint(OB_Maths.locationForRect(0.8f,0.9f,objectDict.get("bottom_bar") .frame()),-20,1.5f,true);
        waitAudio();
        waitForSecs(0.3f);
        index++;
        moveScenePointer(OB_Maths.locationForRect(0.7f,10f,objectDict.get("line") .frame()),-30,0.5f,audioCategory,index,0.5f);
        thePointer.hide();
        startScene(false);
    }

    public void demo_p1_2(OBGroup pointGroup,boolean isColumn) throws Exception
    {
        showAllObjects();
        loadPointer(POINTER_LEFT);
        if(pointGroup != null)
        {
            movePointerToPoint(OB_Maths.locationForRect(0.5f,1.15f,pointGroup.getWorldFrame()),-30,0.5f,true);
            lockScreen();
            if(isColumn)
                highlightColumn(pointGroup,true);
            else
                highlightBlock(pointGroup,true);

            unlockScreen();
            playAudioScene("DEMO",0,true);
            waitForSecs(0.3f);
            lockScreen();
            if(isColumn)
                highlightColumn(pointGroup,false);
            else
                highlightBlock(pointGroup,false);

            unlockScreen();

        }
        moveScenePointer(OB_Maths.locationForRect(1.1f,1.15f,currentBlocks.get(currentBlocks.size()-1).getWorldFrame()),-20,0.5f,"DEMO",1,0.5f);
        thePointer.hide();
        startScene(false);

    }
    public void demo_p1_2b() throws Exception
    {
        List<OBGroup> arr = getCurrentTenColumns();
        demo_p1_2(arr.size()>0 ? arr.get(0) : null,true);
    }

    public void demo_p1_2c() throws Exception
    {
        demo_p1_2(currentBlocks.get(0),false);
    }

    public void demo_p2_1b() throws Exception
    {
        if(phase2Only)
        {
            List<OBGroup> arr = getCurrentTenColumns();
            demo_p1_1(arr.get(0),true,false,"DEMO2");
        }
        else
        {
            demo_p2_1();
        }
    }

    public void demo_p2_1c() throws Exception
    {
        if(phase2Only)
        {
            List<OBGroup> arr = getCurrentHundredBlocks();
            demo_p1_1(arr.get(0),false,false,"DEMO2");
        }
        else
        {
            demo_p2_1();
        }
    }

    public void demo_p2_1() throws Exception
    {
        showAllObjects();
        loadPointer(POINTER_LEFT);
        int index = 0;
        moveScenePointer(OB_Maths.locationForRect(0.9f,0.8f,this.bounds()),-30,0.5f,"DEMO",index,0.3f);
        List<OBGroup> blocks = getCurrentHundredBlocks();
        if(blocks.size() > 0)
        {
            index++;
            PointF loc1 = blocks.get(0).position();
            PointF loc2 = blocks.get(blocks.size()-1).position();
            PointF loc = OB_Maths.locationForRect(0.5f,1.15f,blocks.get(0).getWorldFrame());
            loc.x = loc1.x +(loc2.x - loc1.x) /2.0f;
            moveScenePointer(loc,-30,0.5f,"DEMO",index,0.3f);
            waitAudio();
            waitForSecs(0.3f);
        }

        List<OBGroup> columns = getCurrentTenColumns();
        if(columns.size() > 0)
        {
            index++;
            PointF loc1 = columns.get(0).getWorldPosition();
            PointF loc2 = columns.get(columns.size()-1).getWorldPosition();
            PointF loc = OB_Maths.locationForRect(0.5f,1.15f,columns.get(0).getWorldFrame());
            loc.x = (loc1.x +(loc2.x - loc1.x) /2.0f);
            moveScenePointer(loc,-30,0.5f,"DEMO",index,0.3f);
            waitAudio();
            waitForSecs(0.3f);

        }
        index++;
        List<OBControl> units = getCurrentUnits();
        if(units.size() !=0)
            moveScenePointer(OB_Maths.locationForRect(0.9f,2.5f,units.get(0).getWorldFrame()),-20,0.5f,"DEMO",index,0.5f);
        thePointer.hide();
        startScene(false);
    }

}
