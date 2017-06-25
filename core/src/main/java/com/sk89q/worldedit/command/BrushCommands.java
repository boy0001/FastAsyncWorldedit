/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.BlendBall;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.CircleBrush;
import com.boydti.fawe.object.brush.CommandBrush;
import com.boydti.fawe.object.brush.CopyPastaBrush;
import com.boydti.fawe.object.brush.ErodeBrush;
import com.boydti.fawe.object.brush.FlattenBrush;
import com.boydti.fawe.object.brush.HeightBrush;
import com.boydti.fawe.object.brush.LayerBrush;
import com.boydti.fawe.object.brush.LineBrush;
import com.boydti.fawe.object.brush.PopulateSchem;
import com.boydti.fawe.object.brush.RaiseBrush;
import com.boydti.fawe.object.brush.RecurseBrush;
import com.boydti.fawe.object.brush.ScatterBrush;
import com.boydti.fawe.object.brush.ScatterCommand;
import com.boydti.fawe.object.brush.ScatterOverlayBrush;
import com.boydti.fawe.object.brush.ShatterBrush;
import com.boydti.fawe.object.brush.SplatterBrush;
import com.boydti.fawe.object.brush.SplineBrush;
import com.boydti.fawe.object.brush.StencilBrush;
import com.boydti.fawe.object.brush.SurfaceSphereBrush;
import com.boydti.fawe.object.brush.SurfaceSpline;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.mask.IdMask;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.InvalidUsageException;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

/**
 * Commands to set brush shape.
 */
@Command(aliases = {"brush", "br", "/b"},
        desc = "Команды строения и рисования издалека. [Больше информации](https://github.com/boy0001/FastAsyncWorldedit/wiki/Brushes)"
)
public class BrushCommands extends MethodCommands {

    public BrushCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private BrushSettings get(CommandContext context) throws InvalidToolBindException {
        BrushSettings bs = new BrushSettings();
        bs.addPermissions(getPermissions());
        CommandLocals locals = context.getLocals();
        if (locals != null) {
            String args = (String) locals.get("arguments");
            if (args != null) {
                bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
            }
        }
        return bs;
    }


    @Command(
            aliases = {"blendball", "bb", "blend"},
            usage = "[радиус=5]",
            desc = "Сгладить и смешивает ландшафт",
            help = "Сгладить и смешивает ландшафт\n" +
                    "Картинка: https://i.imgur.com/cNUQUkj.png -> https://i.imgur.com/hFOFsNf.png",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.blendball")
    public BrushSettings blendBallBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new BlendBall()).setSize(radius);
    }

    @Command(
            aliases = {"erode", "e"},
            usage = "[радиус=5]",
            desc = "Подрыть местность",
            help = "Подрыть местность",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.erode")
    public BrushSettings erodeBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new ErodeBrush()).setSize(radius);
    }

    @Command(
            aliases = {"pull"},
            usage = "[радиус=5]",
            desc = "Потянуть рельеф к себе",
            help = "Потянуть рельеф к себе",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.pull")
    public BrushSettings pullBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new RaiseBrush()).setSize(radius);
    }

    @Command(
            aliases = {"circle"},
            usage = "<шаблон> [радиус=5]",
            desc = "Создать круг, который вращается вокруг вашего направления",
            help = "Создать круг, который вращается вокруг вашего направления.\n" +
                    "Заметка: Уменьшить радиус кисти и включить визуализацию, чтобы помочь с размещением в воздухе",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings circleBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new CircleBrush(player)).setSize(radius).setFill(fill);
    }

    @Command(
            aliases = {"recursive", "recurse", "r"},
            usage = "<шаблон-к> [радиус=5]",
            desc = "Установить все подключенные блоки",
            help = "Установить все подключенные блоки\n" +
                    "Флаг -d будет применяться в глубине первого порядка\n" +
                    "Заметка: Установите маску для повторной обработки по определенным блокам",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.brush.recursive")
    public BrushSettings recursiveBrush(Player player, LocalSession session, EditSession editSession, Pattern fill, @Optional("5") double radius, @Switch('d') boolean depthFirst, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new RecurseBrush(depthFirst))
                .setSize(radius)
                .setFill(fill)
                .setMask(new IdMask(editSession));
    }

    @Command(
            aliases = {"line", "l"},
            usage = "<шаблон> [радиус=0]",
            flags = "hsf",
            desc = "Создать линии",
            help =
                    "Создать линии.\n" +
                            "Флаг -h создает только оболочку\n" +
                            "Флаг -s выбирает точку щелчка после рисования\n" +
                            "Флаг -f создает плоскую линию",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.line")
    public BrushSettings lineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("0") double radius, @Switch('h') boolean shell, @Switch('s') boolean select, @Switch('f') boolean flat, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new LineBrush(shell, select, flat))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"spline", "spl", "curve"},
            usage = "<шаблон>",
            desc = "Объединение нескольких объектов вместе на кривой",
            help = "Нажмите, чтобы выбрать некоторые объекты, дважды щелкните один и тот же блок, чтобы подключить объекты.\n" +
                    "Недостаточный радиус кисти или щелчок на неправильном месте приведет к нежелательным формам. Формы должны быть простыми линиями или петлями.\n" +
                    "Картинка1: http://i.imgur.com/CeRYAoV.jpg -> http://i.imgur.com/jtM0jA4.png\n" +
                    "Картинка2: http://i.imgur.com/bUeyc72.png -> http://i.imgur.com/tg6MkcF.png",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.spline")
    public BrushSettings splineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("25") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
        return get(context)
                .setBrush(new SplineBrush(player, session))
                .setSize(radius)
                .setFill(fill);
    }

    // final double tension, final double bias, final double continuity, final double quality

    @Command(
            aliases = {"sspl", "sspline", "surfacespline"},
            usage = "<шаблон> [размер=0] [расстяжение=0] [смещение=0] [сплошность=0] [качество=10]",
            desc = "Рисует сплайн (криволинейная линия) на поверхности",
            help = "Создайте сплайн на поверхности\n" +
                    "Видео: https://www.youtube.com/watch?v=zSN-2jJxXlM",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.surfacespline") // 0, 0, 0, 10, 0,
    public BrushSettings surfaceSpline(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("0") double radius, @Optional("0") double tension, @Optional("0") double bias, @Optional("0") double continuity, @Optional("10") double quality, CommandContext context) throws WorldEditException {
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new SurfaceSpline(tension, bias, continuity, quality))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"sphere", "s"},
            usage = "<шаблон> [радиус=2]",
            flags = "h",
            desc = "Создать сферу",
            help =
                    "Создать сферу.\n" +
                            "Флаг -h создает полые сферы вместо.",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings sphereBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("2") double radius, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        Brush brush;
        if (hollow) {
            brush = new HollowSphereBrush();
        } else {
            brush = new SphereBrush();
        }
        if (fill instanceof BlockPattern) {
            BaseBlock block = ((BlockPattern) fill).getBlock();
            switch (block.getId()) {
                case BlockID.SAND:
                case BlockID.GRAVEL:
                    BBC.BRUSH_TRY_OTHER.send(player);
                    break;
            }
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"shatter", "partition", "split"},
            usage = "<радиус> [радиус=10] [количество=10]",
            desc = "Создает случайные линии, чтобы разбить рельеф на куски",
            help =
                    "Создает случайные линии, чтобы разбить рельеф на куски\n" +
                            "Картинка: https://i.imgur.com/2xKsZf2.png",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.shatter")
    public BrushSettings shatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("10") double radius, @Optional("10") int count, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new ShatterBrush(count))
                .setSize(radius)
                .setFill(fill)
                .setMask(new ExistingBlockMask(editSession));
    }

    @Command(
            aliases = {"stencil", "color"},
            usage = "<шаблон> [радиус=5] [file|#clipboard|imgur=null] [вращение=360] [yscale=1.0]",
            desc = "Используйте карту высоты, чтобы нарисовать поверхность",
            help =
                    "Используйте карту высоты, чтобы нарисовать поверхность.\n" +
                            "Флаг -w будет применяться только при максимальной насыщенности\n" +
                            "Флаг -r будет применять случайное вращение",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.stencil")
    public BrushSettings stencilBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('w') boolean onlyWhite, @Switch('r') boolean randomRotate, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(filename);
        HeightBrush brush;
        try {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null);
        } catch (EmptyClipboardException ignore) {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, null);
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"surface", "surf"},
            usage = "<шаблон> [радиус=5]",
            desc = "Используйте карту высоты, чтобы нарисовать поверхность",
            help =
                    "Используйте карту высоты, чтобы нарисовать поверхность.\n" +
                            "Флаг -w будет применяться только при максимальной насыщенности\n" +
                            "Флаг -r будет применять случайное вращение",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.surface")
    public BrushSettings surfaceBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new SurfaceSphereBrush()).setFill(fill).setSize(radius);
    }

    @Command(
            aliases = {"scatter", "scat"},
            usage = "<шаблон> [радиус=5] [точки=5] [дистанция=1]",
            desc = "Разброс рисунка на поверхности",
            help =
                    "Настройте несколько блоков на поверхности на определенном расстоянии друг от друга.\n" +
                            " Флаг -o накладывает блок\n" +
                            "Видео: https://youtu.be/RPZIaTbqoZw?t=34s",
            flags = "o",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.scatter")
    public BrushSettings scatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("5") double points, @Optional("1") double distance, @Switch('o') boolean overlay, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush;
        if (overlay) {
            brush = new ScatterOverlayBrush((int) points, (int) distance);
        } else {
            brush = new ScatterBrush((int) points, (int) distance);
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"populateschematic", "populateschem", "popschem", "pschem", "ps"},
            usage = "<маска> <file|folder|url> [радиус=30] [точки=5]",
            desc = "Рассеять схематик на поверхности",
            help =
                    "Выбирает схематик разброса.\n" +
                            "Флаг -r будет применять случайное вращение",
            flags = "r",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.brush.populateschematic")
    public BrushSettings scatterSchemBrush(Player player, EditSession editSession, LocalSession session, Mask mask, String clipboard, @Optional("30") double radius, @Optional("50") double density, @Switch('r') boolean rotate, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);


        try {
            ClipboardHolder[] clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(player, player.getWorld().getWorldData(), clipboard, true);
            if (clipboards == null) {
                return null;
            }
            return get(context)
                    .setBrush(new PopulateSchem(mask, clipboards, (int) density, rotate))
                    .setSize(radius);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            aliases = {"layer"},
            usage = "<радиус> [color|<шаблон1> <patern2>...]",
            desc = "Заменить ландшафт слоем.",
            help = "Заменить ландшафт слоем.\n" +
                    "Например: /br layer 5 95:1 95:2 35:15 - Накладывает несколько слоев на поверхность\n" +
                    "Картинка: https://i.imgur.com/XV0vYoX.png",
            min = 0,
            max = 999
    )
    @CommandPermissions("worldedit.brush.layer")
    public BrushSettings surfaceLayer(Player player, EditSession editSession, LocalSession session, double radius, CommandContext args, CommandContext context) throws WorldEditException, InvalidUsageException {
        worldEdit.checkMaxBrushRadius(radius);
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        List<BaseBlock> blocks = new ArrayList<>();
        if (args.argsLength() < 2) {
            throw new InvalidUsageException(getCallable());
        }
        try {
            Color color = Color.web(args.getString(1));
            java.awt.Color awtColor = new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) color.getOpacity());
            char[] glassLayers = Fawe.get().getTextureUtil().getNearestLayer(awtColor.getRGB());
            for (char layer : glassLayers) {
                blocks.add(FaweCache.CACHE_BLOCK[layer]);
            }
        } catch (IllegalArgumentException ignore) {
            for (int i = 1; i < args.argsLength(); i++) {
                String arg = args.getString(i);
                blocks.add(worldEdit.getBlockFactory().parseFromInput(arg, parserContext));
            }
        }
        return get(context)
                .setBrush(new LayerBrush(blocks.toArray(new BaseBlock[blocks.size()])))
                .setSize(radius);
    }

    @Command(
            aliases = {"splatter", "splat"},
            usage = "<шаблон> [радиус=5] [сид=1] [рекурсия=5] [твердость=true]",
            desc = "Разбрызгивание рисунка на поверхности",
            help = "Устанавливает кучу блоков случайным образом на поверхности.\n" +
                    "Картинка: https://i.imgur.com/hMD29oO.png\n" +
                    "Например: /br splatter stone,dirt 30 15\n" +
                    "Заметка: Семена определяют, сколько фрагментов есть, рекурсия определяет, как большой, твердый определяет, применяется ли шаблон для каждого семени, иначе для каждого блока.",
            min = 1,
            max = 5
    )
    @CommandPermissions("worldedit.brush.splatter")
    public BrushSettings splatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("1") double points, @Optional("5") double recursion, @Optional("true") boolean solid, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new SplatterBrush((int) points, (int) recursion, solid))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"scmd", "scattercmd", "scattercommand", "scommand"},
            usage = "<радиус-разброса> <точки> <cmd-radius=1> <cmd1;cmd2...>",
            desc = "Выполнять команды в случайных точках на поверхности",
            help =
                    "Выполнять команды в случайных точках на поверхности\n" +
                            " - Радиус рассеяния - это минимальное расстояние между каждой точкой\n" +
                            " - Ваш выбор будет расширен до указанного размера вокруг каждой точки\n" +
                            " - Заполнители: {x}, {y}, {z}, {world}, {size}",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.scattercommand")
    public BrushSettings scatterCommandBrush(Player player, EditSession editSession, LocalSession session, double radius, double points, double distance, CommandContext args, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new ScatterCommand((int) points, (int) distance, args.getJoinedStrings(3)))
                .setSize(radius);
    }

    @Command(
            aliases = {"cylinder", "cyl", "c"},
            usage = "<блок> [радиус=2] [высота=1]",
            flags = "h",
            desc = "Создать цилиндр",
            help =
                    "Создать цилиндр.\n" +
                            "Флаг -h создает полые цилиндры.",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public BrushSettings cylinderBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
                                       @Optional("2") double radius, @Optional("1") int height, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);
        BrushSettings settings = get(context);

        if (hollow) {
            settings.setBrush(new HollowCylinderBrush(height));
        } else {
            settings.setBrush(new CylinderBrush(height));
        }
        settings.setSize(radius)
                .setFill(fill);
        return settings;
    }

    @Command(
            aliases = {"clipboard"},
            usage = "",
            desc = "Выбрать кисть в буфере обмена (Рекомендуемое: `/br copypaste`)",
            help =
                    "Выбрать кисть в буфере обмена.\n" +
                            "Флаг -a заставляет его не вставлять воздух.\n" +
                            "Без флага -p вставка будет отображаться по центру в целевом местоположении. " +
                            "С флагом, вставка будет отображаться относительно того, где вы " +
                            "стояли относительно скопированной области, когда вы ее скопировали."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public BrushSettings clipboardBrush(Player player, LocalSession session, @Switch('a') boolean ignoreAir, @Switch('p') boolean usingOrigin, CommandContext context) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        Vector size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.getBlockX());
        worldEdit.checkMaxBrushRadius(size.getBlockY());
        worldEdit.checkMaxBrushRadius(size.getBlockZ());
        return get(context).setBrush(new ClipboardBrush(holder, ignoreAir, usingOrigin));
    }

    @Command(
            aliases = {"smooth"},
            usage = "[размер=2] [интерации=4]",
            flags = "n",
            desc = "Плавный ландшафт (Рекомендуемое: `/br blendball`)",
            help =
                    "Выбирает щетку для смягчения ландшафта.\n" +
                            "Флаг -n позволяет рассматривать только естественные блоки.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.smooth")
    public BrushSettings smoothBrush(Player player, LocalSession session, EditSession editSession,
                                     @Optional("2") double radius, @Optional("4") int iterations, @Switch('n')
                                             boolean naturalBlocksOnly, CommandContext context) throws WorldEditException {

        worldEdit.checkMaxBrushRadius(radius);

        FawePlayer fp = FawePlayer.wrap(player);
        FaweLimit limit = Settings.IMP.getLimit(fp);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);

        return get(context)
                .setBrush(new SmoothBrush(iterations, naturalBlocksOnly))
                .setSize(radius);
    }

    @Command(
            aliases = {"ex", "extinguish"},
            usage = "[радиус=5]",
            desc = "Яркая огнетушительная щетка",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.ex")
    public BrushSettings extinguishBrush(Player player, LocalSession session, EditSession editSession, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        Pattern fill = new BlockPattern(new BaseBlock(0));
        return get(context)
                .setBrush(new SphereBrush())
                .setSize(radius)
                .setFill(fill)
                .setMask(new BlockMask(editSession, new BaseBlock(BlockID.FIRE)));
    }

    @Command(
            aliases = {"gravity", "grav"},
            usage = "[радиус=5]",
            flags = "h",
            desc = "Гравитационная кисть",
            help =
                    "Эта кисть имитирует влияние гравитации.\n" +
                            "Флаг -h влияет на блоки, начинающиеся с максимума в мире y, " +
                            "вместо радиуса y + блока щелчка.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.gravity")
    public BrushSettings gravityBrush(Player player, LocalSession session, @Optional("5") double radius, @Switch('h') boolean fromMaxY, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        return get(context)
                .setBrush(new GravityBrush(fromMaxY))
                .setSize(radius);
    }

    @Command(
            aliases = {"height", "heightmap"},
            usage = "[радиус=5] [file|#clipboard|imgur=null] [поворот=0] [yscale=1.00]",
            flags = "h",
            desc = "Поднять или уменьшить ландшафт с помощью карты высот",
            help =
                    "Эта кисть поднимает и опускает землю.\n" +
                            " - Флаг `-r` позволяет случайное вращение вне оси\n" +
                            " - Флаг `-l` будет работать на слоях снега\n" +
                            " - Флаг `-s` отключает сглаживание\n" +
                            "Заметка: Используйте отрицательный yscale для уменьшения высоты\n" +
                            "Картинка: https://i.imgur.com/Hrzn0I4.png",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings heightBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, filename, rotation, yscale, false, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    @Command(
            aliases = {"cliff", "flatcylinder"},
            usage = "[радиус=5] [file|#clipboard|imgur=null] [вращение=0] [yscale=1.00]",
            flags = "h",
            desc = "Клифф-кисть",
            help =
                    "Эта кисть выравнивает рельеф местности и создает скалы.\n" +
                            " - Флаг `-r` позволяет случайное вращение вне оси\n" +
                            " - Флаг `-l` будет работать на слоях снега\n" +
                            " - Флаг `-s` отключает сглаживание",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings cliffBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, filename, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CYLINDER, context);
    }

    @Command(
            aliases = {"flatten", "flatmap", "flat"},
            usage = "[радиус=5] [file|#clipboard|imgur=null] [вращение=0] [yscale=1.00]",
            flags = "h",
            help = "Сглаживающая кисть выравнивает рельеф\n" +
                    " - Флаг `-r` позволяет случайное вращение вне оси\n" +
                    " - Флаг `-l` будет работать на слоях снега\n" +
                    " - Флаг `-s` отключает сглаживание",
            desc = "Эта кисть поднимает и опускает землю в направлении щелкнутой точки\n",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings flattenBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, filename, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    private InputStream getHeightmapStream(String filename) {
        String filenamePng = (filename.endsWith(".png") ? filename : filename + ".png");
        File file = new File(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HEIGHTMAP + File.separator + filenamePng);
        if (!file.exists()) {
            if (!filename.equals("#clipboard") && filename.length() >= 7) {
                try {
                    URL url;
                    if (filename.startsWith("http")) {
                        url = new URL(filename);
                        if (!url.getHost().equals("i.imgur.com")) {
                            throw new FileNotFoundException(filename);
                        }
                    } else {
                        url = new URL("https://i.imgur.com/" + filenamePng);
                    }
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    return Channels.newInputStream(rbc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (!filename.equalsIgnoreCase("#clipboard")) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private BrushSettings terrainBrush(Player player, LocalSession session, double radius, String filename, int rotation, double yscale, boolean flat, boolean randomRotate, boolean layers, boolean smooth, ScalableHeightMap.Shape shape, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(filename);
        HeightBrush brush;
        if (flat) {
            try {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null, shape);
            } catch (EmptyClipboardException ignore) {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, null, shape);
            }
        } else {
            try {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null);
            } catch (EmptyClipboardException ignore) {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, null);
            }
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius);
    }


    @Command(
            aliases = {"copypaste", "copy", "paste", "cp", "copypasta"},
            usage = "[глубина=5]",
            desc = "Копировать вставить кисть",
            help = "Щелкните левой кнопкой мыши по основанию объекта для копирования.\n" +
                    "Щелкните правой кнопкой мыши, чтобы вставить\n" +
                    "Флаг -r применит случайное вращение на вставке\n" +
                    "Заметка: Хорошо работает с действием прокрутки буфера обмена\n" +
                    "Видео: https://www.youtube.com/watch?v=RPZIaTbqoZw",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.copy")
    public BrushSettings copy(Player player, LocalSession session, @Optional("5") double radius, @Switch('r') boolean rotate, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_COPY.f(radius));
        return get(context)
                .setBrush(new CopyPastaBrush(player, session, rotate))
                .setSize(radius);
    }

    @Command(
            aliases = {"command", "cmd"},
            usage = "<радиус> [cmd1;cmd2...]",
            desc = "Кисть команд",
            help =
                    "Запуск команд в нажатом положении.\n" +
                            " - Ваш выбор будет расширен до указанного размера вокруг каждой точки\n" +
                            " - Заполнители: {x}, {y}, {z}, {world}, {size}",

            min = 2,
            max = 99
    )
    @CommandPermissions("worldedit.brush.command")
    public BrushSettings command(Player player, LocalSession session, double radius, CommandContext args, CommandContext context) throws WorldEditException {
        String cmd = args.getJoinedStrings(1);
        return get(context)
                .setBrush(new CommandBrush(cmd, radius))
                .setSize(radius);
    }

    @Command(
            aliases = {"butcher", "kill"},
            usage = "[радиус=5]",
            flags = "plangbtfr",
            desc = "Мясная щетка",
            help = "Убивает близлежащих мобов в указанном радиусе.\n" +
                    "Флаги:\n" +
                    "  -p убивает питомцев.\n" +
                    "  -n убивает NPC.\n" +
                    "  -g убивает Големов.\n" +
                    "  -a убивает животных.\n" +
                    "  -b убивает окружающих мобов.\n" +
                    "  -t убивает мобов с именами.\n" +
                    "  -f обьеденяет все предыдущие флаги\n" +
                    "  -r убивает стойки.\n" +
                    "  -l в настоящее время ничего не делает.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.butcher")
    public BrushSettings butcherBrush(Player player, LocalSession session, CommandContext args, CommandContext context) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        double radius = args.argsLength() > 0 ? args.getDouble(0) : 5;
        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius && maxRadius != -1) {
            BBC.TOOL_RADIUS_ERROR.send(player, maxRadius);
            return null;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        return get(context)
                .setBrush(new ButcherBrush(flags))
                .setSize(radius);
    }

    public static Class<?> inject() {
        return BrushCommands.class;
    }
}