package wtf.dettex.api.system.font.entry;

import wtf.dettex.api.system.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
