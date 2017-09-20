package com.boydti.fawe.bukkit.wrapper.state;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncBlockState;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import java.util.Map;
import org.bukkit.block.Sign;

public class AsyncSign extends AsyncBlockState implements Sign {
    public AsyncSign(AsyncBlock block, int combined) {
        super(block, combined);
    }

    @Override
    public String[] getLines() {
        CompoundTag nbt = getNbtData();
        String[] data = new String[4];
        if (nbt != null) {
            for (int i = 1; i <= 4; i++) {
                data[i - 1] = nbt.getString("Text" + i);
            }
        }
        return data;
    }

    @Override
    public String getLine(int index) throws IndexOutOfBoundsException {
        CompoundTag nbt = getNbtData();
        return nbt == null ? null : nbt.getString("Text" + (index + 1));
    }

    @Override
    public void setLine(int index, String line) throws IndexOutOfBoundsException {
        CompoundTag nbt = getNbtData();
        if (nbt != null) {
            Map<String, Tag> map = ReflectionUtils.getMap(nbt.getValue());
            map.put("Text" + (index + 1), new StringTag(line));
        }
    }
}
