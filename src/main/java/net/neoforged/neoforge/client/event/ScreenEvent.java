package net.neoforged.neoforge.client.event;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.Event;

public class ScreenEvent extends Event {
    private final Screen screen;
    protected ScreenEvent(Screen screen) { this.screen=screen; }
    public Screen getScreen() { return screen; }

    public static final class Init {
        public static final class Post extends ScreenEvent {
            private final List<GuiEventListener> listeners;
            private final Consumer<GuiEventListener> add;
            private final Consumer<GuiEventListener> remove;
            public Post(Screen screen, List<GuiEventListener> listeners,
                        Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
                super(screen); this.listeners=listeners; this.add=add; this.remove=remove;
            }
            public List<GuiEventListener> getListenersList() { return List.copyOf(listeners); }
            public void addListener(GuiEventListener listener) { add.accept(listener); }
            public void removeListener(GuiEventListener listener) { remove.accept(listener); }
        }
    }
    public static final class Render {
        public static class Pre extends ScreenEvent {
            private final GuiGraphics graphics; private final int mouseX, mouseY;
            public Pre(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
                super(screen); this.graphics=graphics; this.mouseX=mouseX; this.mouseY=mouseY;
            }
            public GuiGraphics getGuiGraphics(){return graphics;}
            public int getMouseX(){return mouseX;} public int getMouseY(){return mouseY;}
        }
        public static final class Post extends Pre {
            public Post(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) { super(screen,graphics,mouseX,mouseY); }
        }
    }
    public abstract static class MouseButton extends ScreenEvent {
        private final double mouseX, mouseY; private final int button;
        protected MouseButton(Screen screen,double mouseX,double mouseY,int button){super(screen);this.mouseX=mouseX;this.mouseY=mouseY;this.button=button;}
        public double getMouseX(){return mouseX;} public double getMouseY(){return mouseY;} public int getButton(){return button;}
    }
    public static final class MouseButtonPressed { public static final class Pre extends MouseButton { public Pre(Screen s,double x,double y,int b){super(s,x,y,b);} } }
    public static final class MouseButtonReleased { public static final class Pre extends MouseButton { public Pre(Screen s,double x,double y,int b){super(s,x,y,b);} } }
    public static final class MouseDragged {
        public static final class Pre extends MouseButton {
            private final double dragX,dragY;
            public Pre(Screen s,double x,double y,int b,double dx,double dy){super(s,x,y,b);dragX=dx;dragY=dy;}
            public int getMouseButton(){return getButton();} public double getDragX(){return dragX;} public double getDragY(){return dragY;}
        }
    }
    public static final class MouseScrolled {
        public static final class Pre extends ScreenEvent {
            private final double mouseX,mouseY,deltaX,deltaY;
            public Pre(Screen s,double x,double y,double dx,double dy){super(s);mouseX=x;mouseY=y;deltaX=dx;deltaY=dy;}
            public double getMouseX(){return mouseX;} public double getMouseY(){return mouseY;}
            public double getScrollDeltaX(){return deltaX;} public double getScrollDeltaY(){return deltaY;}
        }
    }
    public static final class KeyPressed {
        public static final class Pre extends ScreenEvent {
            private final int keyCode,scanCode,modifiers;
            public Pre(Screen s,int key,int scan,int modifiers){super(s);keyCode=key;scanCode=scan;this.modifiers=modifiers;}
            public int getKeyCode(){return keyCode;} public int getScanCode(){return scanCode;} public int getModifiers(){return modifiers;}
        }
    }
}
