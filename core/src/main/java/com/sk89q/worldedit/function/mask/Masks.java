package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.session.request.Request;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Various utility functions related to {@link Mask} and {@link Mask2D}.
 */
public final class Masks {

    private static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();
    private static final AlwaysFalse ALWAYS_FALSE = new AlwaysFalse();

    private Masks() {
    }

    public static boolean isNull(Mask mask) {
        return mask == null || mask == ALWAYS_TRUE;
    }

    /**
     * Return a 3D mask that always returns true;
     *
     * @return a mask
     */
    public static Mask alwaysTrue() {
        return ALWAYS_TRUE;
    }

    public static Mask alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Return a 2D mask that always returns true;
     *
     * @return a mask
     */
    public static Mask2D alwaysTrue2D() {
        return ALWAYS_TRUE;
    }

    /**
     * Negate the given mask.
     *
     * @param finalMask the mask
     * @return a new mask
     */
    public static Mask negate(final Mask finalMask) {
        if (finalMask instanceof AlwaysTrue) {
            return ALWAYS_FALSE;
        } else if (finalMask instanceof AlwaysFalse) {
            return ALWAYS_TRUE;
        }
        checkNotNull(finalMask);
        return new AbstractMask() {
            private Mask mask = finalMask;

            @Override
            public boolean test(Vector vector) {
                return !mask.test(vector);
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                Mask2D mask2d = mask.toMask2D();
                if (mask2d != null) {
                    return negate(mask2d);
                } else {
                    return null;
                }
            }
        };
    }

    /**
     * Negate the given mask.
     *
     * @param mask the mask
     * @return a new mask
     */
    public static Mask2D negate(final Mask2D mask) {
        if (mask instanceof AlwaysTrue) {
            return ALWAYS_FALSE;
        } else if (mask instanceof AlwaysFalse) {
            return ALWAYS_TRUE;
        }

        checkNotNull(mask);
        return new AbstractMask2D() {
            @Override
            public boolean test(Vector2D vector) {
                return !mask.test(vector);
            }
        };
    }

    /**
     * Return a 3-dimensional version of a 2D mask.
     *
     * @param mask the mask to make 3D
     * @return a 3D mask
     */
    public static Mask asMask(final Mask2D mask) {
        return new AbstractMask() {
            @Override
            public boolean test(Vector vector) {
                return mask.test(vector.toVector2D());
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return mask;
            }
        };
    }

    /**
     * Wrap an old-style mask and convert it to a new mask.
     * <p>
     * <p>Note, however, that this is strongly not recommended because
     * {@link com.sk89q.worldedit.masks.Mask#prepare(LocalSession, LocalPlayer, Vector)}
     * is not called.</p>
     *
     * @param mask        the old-style mask
     * @param editSession the edit session to bind to
     * @return a new-style mask
     * @deprecated Please avoid if possible
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Mask wrap(final com.sk89q.worldedit.masks.Mask mask, final EditSession editSession) {
        checkNotNull(mask);
        return new AbstractMask() {
            @Override
            public boolean test(Vector vector) {
                return mask.matches(editSession, vector);
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return null;
            }
        };
    }

    /**
     * Wrap an old-style mask and convert it to a new mask.
     * <p>
     * <p>As an {@link EditSession} is not provided in this case, one will be
     * taken from the {@link Request}, if possible. If not possible, then the
     * mask will return false.</p>
     *
     * @param mask the old-style mask
     * @return a new-style mask
     */
    @SuppressWarnings("deprecation")
    public static Mask wrap(final com.sk89q.worldedit.masks.Mask mask) {
        checkNotNull(mask);
        return new AbstractMask() {
            @Override
            public boolean test(Vector vector) {
                EditSession editSession = Request.request().getEditSession();
                return editSession != null && mask.matches(editSession, vector);
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return null;
            }
        };
    }

    /**
     * Convert a new-style mask to an old-style mask.
     *
     * @param mask the new-style mask
     * @return an old-style mask
     */
    @SuppressWarnings("deprecation")
    public static com.sk89q.worldedit.masks.Mask wrap(final Mask mask) {
        checkNotNull(mask);
        return new com.sk89q.worldedit.masks.AbstractMask() {
            @Override
            public boolean matches(EditSession editSession, Vector position) {
                Request.request().setEditSession(editSession);
                return mask.test(position);
            }
        };
    }

    private static class AlwaysTrue implements Mask, Mask2D {
        @Override
        public boolean test(Vector vector) {
            return true;
        }

        @Override
        public boolean test(Vector2D vector) {
            return true;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }
    }

    private static class AlwaysFalse implements Mask, Mask2D {
        @Override
        public boolean test(Vector vector) {
            return false;
        }

        @Override
        public boolean test(Vector2D vector) {
            return false;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }
    }

    public static Class<?> inject() {
        return Masks.class;
    }
}