package dev.zedkodex.modehud.client;

import java.util.Random;

public class AsciiField {
    public enum Flow { NONE, UP, DOWN, LEFT, RIGHT }

    public static final String GLYPHS = "01#@$%&*+=<>/\\|{}[]?!";

    public final int cols;
    public final int rows;
    public final char[] g;
    private final String glyphs;
    private final Flow flow;
    private final float density;
    private final Random r = new Random();

    public AsciiField(int cols, int rows, String glyphs, Flow flow, float density) {
        this.cols = cols;
        this.rows = rows;
        this.glyphs = glyphs;
        this.flow = flow;
        this.density = density;
        this.g = new char[cols * rows];
        for (int i = 0; i < g.length; i++) g[i] = cell();
    }

    private char cell() {
        return r.nextFloat() < density ? glyphs.charAt(r.nextInt(glyphs.length())) : ' ';
    }

    public void step() {
        switch (flow) {
            case DOWN -> {
                System.arraycopy(g, 0, g, cols, cols * (rows - 1));
                for (int c = 0; c < cols; c++) g[c] = cell();
            }
            case UP -> {
                System.arraycopy(g, cols, g, 0, cols * (rows - 1));
                for (int c = 0; c < cols; c++) g[cols * (rows - 1) + c] = cell();
            }
            case RIGHT -> {
                for (int row = 0; row < rows; row++) {
                    System.arraycopy(g, row * cols, g, row * cols + 1, cols - 1);
                    g[row * cols] = cell();
                }
            }
            case LEFT -> {
                for (int row = 0; row < rows; row++) {
                    System.arraycopy(g, row * cols + 1, g, row * cols, cols - 1);
                    g[(row + 1) * cols - 1] = cell();
                }
            }
            default -> {
                int n = (int) (g.length * 0.12f);
                for (int k = 0; k < n; k++) g[r.nextInt(g.length)] = cell();
            }
        }
    }
}
