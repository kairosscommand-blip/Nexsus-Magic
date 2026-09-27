package com.nexusuniverse.magic;

/** The two item shapes every element comes in. Both behave identically once crafted -- the same
 * cast/impact logic fires either one -- they only differ in their base Material and recipe shape
 * (see MagicItemFactory / RecipeRegistrar). */
public enum ItemKind {
    STAFF,
    BOW
}
