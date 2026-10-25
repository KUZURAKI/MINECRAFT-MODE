package dev.zedkodex.modehud.client;

public final class Scramble {
    private Scramble() {}

    public static char glyph(String glyphs, int f, int i, int salt) {
        return glyphs.charAt(Gfx.hash(f * 7919 + i * 104729 + salt * 31) % glyphs.length());
    }

    public static String text(String s, long elapsedMs, int startFrames, float rate, String glyphs, int salt) {
        int f = (int) (elapsedMs / 40);
        float done = (f - startFrames) * rate;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                sb.append(' ');
            } else if (i >= done) {
                sb.append(f < startFrames ? ' ' : glyph(glyphs, f, i, salt));
            } else if (f < 170 && Gfx.hash(f * 31 + i * 17 + salt) % 1000 < 15) {
                sb.append(glyph(glyphs, f, i, salt));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
