package com.minemaarten.signals.client.gui.widget;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.MathHelper;

import org.apache.commons.lang3.math.NumberUtils;

public class WidgetTextFieldNumber extends WidgetTextField{

    public int minValue = Integer.MIN_VALUE;
    public int maxValue = Integer.MAX_VALUE;
    private int decimals;

    public WidgetTextFieldNumber(FontRenderer fontRenderer, int x, int y, int width, int height){
        super(fontRenderer, x, y, width, height);
        setValue(0);

        setValidator(input -> {
            if(input == null || input.isEmpty() || input.equals("-")) {
                return true; // treat as numeric zero
            }
            try {
                double d = Double.parseDouble(input);
                return d >= this.minValue && d <= this.maxValue;
            } catch(NumberFormatException e) {
                return false;
            }
        });
    }

    public WidgetTextFieldNumber setDecimals(int decimals){
        this.decimals = decimals;
        return this;
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button){
        boolean wasFocused = isFocused();
        super.onMouseClicked(mouseX, mouseY, button);
        if(isFocused()) {
            if(!wasFocused) { //setText("");
                setCursorPositionEnd();
                setSelectionPos(0);
            }
        } else {
            setValue(getDoubleValue());
        }
    }

    public WidgetTextFieldNumber setValue(double value){
        setText(roundNumberTo(value, decimals));
        return this;
    }

    public int getValue(){
        return MathHelper.clamp(NumberUtils.toInt(getText()), minValue, maxValue);
    }

    public double getDoubleValue(){
        return roundNumberToDouble(MathHelper.clamp(NumberUtils.toDouble(getText()), minValue, maxValue), decimals);
    }

    /**
     * Rounds numbers down at the given decimal. 1.234 with decimal 1 will result in a string holding "1.2"
     *
     * @param value
     * @param decimals
     * @return
     */
    private static String roundNumberTo(double value, int decimals){
        double ret = roundNumberToDouble(value, decimals);
        if(decimals == 0) {
            return "" + (int)ret;
        } else {
            return "" + ret;
        }
    }

    private static double roundNumberToDouble(double value, int decimals){
        return Math.round(value * Math.pow(10, decimals)) / Math.pow(10, decimals);
    }
}
