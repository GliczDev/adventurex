/*
 * This file is part of adventurex, licensed under the MIT License.
 *
 * Copyright (c) 2017-2026 PaperMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.papermc.adventurex.nbt.dfu;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.NumberBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

final class BinaryTagOpsImpl implements BinaryTagOps {
  static final BinaryTagOpsImpl INSTANCE = new BinaryTagOpsImpl();

  static String unwrapString(final BinaryTag tag) {
    if (tag instanceof final StringBinaryTag stringBinaryTag) {
      return stringBinaryTag.value();
    } else {
      throw new IllegalArgumentException("Expected a string, got " + tag.type());
    }
  }

  static DataResult<String> unwrapStringResult(final BinaryTag tag) {
    if (tag instanceof final StringBinaryTag stringBinaryTag) {
      return DataResult.success(stringBinaryTag.value());
    } else {
      return DataResult.error(() -> "Expected a string, got " + tag.type());
    }
  }

  @Override
  public BinaryTag empty() {
    return CompoundBinaryTag.empty();
  }

  @Override
  public <U> U convertTo(final DynamicOps<U> outOps, final BinaryTag input) {
    return switch (input) {
      case CompoundBinaryTag compoundBinaryTag -> this.convertMap(outOps, compoundBinaryTag);
      case ListBinaryTag listBinaryTag -> this.convertList(outOps, listBinaryTag);
      case ByteArrayBinaryTag byteArrayBinaryTag -> outOps.createByteList(ByteBuffer.wrap(byteArrayBinaryTag.value()));
      case IntArrayBinaryTag intArrayBinaryTag -> outOps.createIntList(intArrayBinaryTag.stream());
      case LongArrayBinaryTag longArrayBinaryTag -> outOps.createLongList(longArrayBinaryTag.stream());
      case StringBinaryTag stringBinaryTag -> outOps.createString(stringBinaryTag.value());
      case ByteBinaryTag byteBinaryTag -> outOps.createByte(byteBinaryTag.byteValue());
      case ShortBinaryTag shortBinaryTag -> outOps.createShort(shortBinaryTag.shortValue());
      case IntBinaryTag intBinaryTag -> outOps.createInt(intBinaryTag.intValue());
      case LongBinaryTag longBinaryTag -> outOps.createLong(longBinaryTag.longValue());
      case FloatBinaryTag floatBinaryTag -> outOps.createFloat(floatBinaryTag.floatValue());
      case DoubleBinaryTag doubleBinaryTag -> outOps.createDouble(doubleBinaryTag.doubleValue());
      case EndBinaryTag ignored -> outOps.empty();
    };
  }

  @Override
  public DataResult<Number> getNumberValue(final BinaryTag input) {
    if (input instanceof final NumberBinaryTag numberBinaryTag) {
      return DataResult.success(numberBinaryTag.numberValue());
    } else {
      return DataResult.error(() -> "Expected a number, got " + input.type());
    }
  }

  @Override
  public BinaryTag createNumeric(final Number i) {
    return switch (i) {
      case final Byte b -> ByteBinaryTag.byteBinaryTag(b);
      case final Short s -> ShortBinaryTag.shortBinaryTag(s);
      case final Integer j -> IntBinaryTag.intBinaryTag(j);
      case final Long l -> LongBinaryTag.longBinaryTag(l);
      case final Float f -> FloatBinaryTag.floatBinaryTag(f);
      case final Double d -> DoubleBinaryTag.doubleBinaryTag(d);
      default -> DoubleBinaryTag.doubleBinaryTag(i.doubleValue());
    };
  }

  @Override
  public DataResult<Boolean> getBooleanValue(final BinaryTag input) {
    if (input instanceof final ByteBinaryTag byteBinaryTag) {
      return DataResult.success(byteBinaryTag.byteValue() != 0);
    } else {
      return DataResult.error(() -> "Expected a boolean, got " + input.type());
    }
  }

  @Override
  public BinaryTag createBoolean(final boolean value) {
    return ByteBinaryTag.byteBinaryTag((byte) (value ? 1 : 0));
  }

  @Override
  public DataResult<String> getStringValue(final BinaryTag input) {
    if (input instanceof final StringBinaryTag stringBinaryTag) {
      return DataResult.success(stringBinaryTag.value());
    } else {
      return DataResult.error(() -> "Expected a string, got " + input.type());
    }
  }

  @Override
  public BinaryTag createString(final String value) {
    return StringBinaryTag.stringBinaryTag(value);
  }

  @Override
  public DataResult<BinaryTag> mergeToList(final BinaryTag list, final BinaryTag value) {
    if (list instanceof final ListBinaryTag listBinaryTag) {
      return DataResult.success(listBinaryTag.add(value));
    } else if (list.equals(this.empty())) {
      return DataResult.success(ListBinaryTag.listBinaryTag(value.type(), List.of(value)));
    } else {
      return DataResult.error(() -> "Expected a list, got " + list.type());
    }
  }

  @Override
  public DataResult<BinaryTag> mergeToList(final BinaryTag list, final List<BinaryTag> values) {
    if (list instanceof final ListBinaryTag listBinaryTag) {
      return DataResult.success(listBinaryTag.add(values));
    } else if (list.equals(this.empty())) {
      BinaryTagType<? extends BinaryTag> singleType = null;
      for (final BinaryTag tag : values) {
        final BinaryTagType<?> thisType = tag.type();
        if (singleType == null) {
          singleType = thisType;
        } else if (singleType != thisType) {
          singleType = null;
          break;
        }
      }
      if (singleType != null) {
        return DataResult.success(ListBinaryTag.listBinaryTag(singleType, values));
      } else {
        return DataResult.success(ListBinaryTag.heterogeneousListBinaryTag(values.size()).add(values).build());
      }
    } else {
      return DataResult.error(() -> "Expected a list, got " + list.type());
    }
  }

  @Override
  public DataResult<BinaryTag> mergeToMap(final BinaryTag map, final BinaryTag key, final BinaryTag value) {
    if (map.equals(this.empty())) {
      return unwrapStringResult(key).map(keyString -> CompoundBinaryTag.from(Map.of(keyString, value)));
    } else if (!(map instanceof final CompoundBinaryTag compoundBinaryTag)) {
      return DataResult.error(() -> "Expected a compound, got " + map.type());
    } else {
      return unwrapStringResult(key).map(keyString -> compoundBinaryTag.put(keyString, value));
    }
  }

  @Override
  public DataResult<BinaryTag> mergeToMap(final BinaryTag map, final Map<BinaryTag, BinaryTag> values) {
    if (map.equals(this.empty())) {
      return DataResult.success(values.entrySet().stream().collect(CompoundBinaryTag.toCompoundTag(entry -> unwrapString(entry.getKey()), Map.Entry::getValue)));
    } else if (!(map instanceof final CompoundBinaryTag compoundBinaryTag)) {
      return DataResult.error(() -> "Expected a compound, got " + map.type());
    } else {
      return DataResult.success(compoundBinaryTag.put(values.entrySet().stream().map(entry -> Pair.of(unwrapString(entry.getKey()), entry.getValue())).collect(Pair.toMap())));
    }
  }

  @Override
  public DataResult<BinaryTag> mergeToMap(final BinaryTag map, final MapLike<BinaryTag> values) {
    if (map.equals(this.empty())) {
      return DataResult.success(values.entries().collect(CompoundBinaryTag.toCompoundTag(entry -> unwrapString(entry.getFirst()), Pair::getSecond)));
    } else if (!(map instanceof final CompoundBinaryTag compoundBinaryTag)) {
      return DataResult.error(() -> "Expected a compound, got " + map.type());
    } else {
      return DataResult.success(compoundBinaryTag.put(values.entries().map(entry -> entry.mapFirst(BinaryTagOpsImpl::unwrapString)).collect(Pair.toMap())));
    }
  }

  @Override
  public DataResult<MapLike<BinaryTag>> getMap(final BinaryTag input) {
    if (input instanceof final CompoundBinaryTag compoundBinaryTag) {
      return DataResult.success(new CompoundMapLike(compoundBinaryTag));
    } else {
      return DataResult.error(() -> "Expected a compound, got " + input.type());
    }
  }

  @Override
  public DataResult<Stream<Pair<BinaryTag, BinaryTag>>> getMapValues(final BinaryTag input) {
    if (input instanceof final CompoundBinaryTag compoundBinaryTag) {
      return DataResult.success(compoundBinaryTag.stream()
          .map(entry -> Pair.of(StringBinaryTag.stringBinaryTag(entry.getKey()), entry.getValue())));
    } else {
      return DataResult.error(() -> "Expected a compound, got " + input.type());
    }
  }

  @Override
  public BinaryTag createMap(final Stream<Pair<BinaryTag, BinaryTag>> map) {
    final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
    map.forEach(entry -> builder.put(unwrapString(entry.getFirst()), entry.getSecond()));
    return builder.build();
  }

  @Override
  public DataResult<Stream<BinaryTag>> getStream(final BinaryTag input) {
    if (input instanceof final ListBinaryTag listBinaryTag) {
      return DataResult.success(listBinaryTag.stream());
    } else {
      return DataResult.error(() -> "Expected a list, got " + input.type());
    }
  }

  @Override
  public BinaryTag createList(final Stream<BinaryTag> input) {
    final ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.builder();
    input.forEach(builder::add);
    return builder.build();
  }

  @Override
  public DataResult<ByteBuffer> getByteBuffer(final BinaryTag input) {
    if (input instanceof final ByteArrayBinaryTag byteArrayBinaryTag) {
      return DataResult.success(ByteBuffer.wrap(byteArrayBinaryTag.value()));
    } else {
      return DataResult.error(() -> "Expected a byte array, got " + input.type());
    }
  }

  @SuppressWarnings("ByteBufferBackingArray")
  @Override
  public BinaryTag createByteList(final ByteBuffer input) {
    if (input.hasArray()) {
      return ByteArrayBinaryTag.byteArrayBinaryTag(input.array());
    } else {
      final byte[] bytes = new byte[input.remaining()];
      input.get(bytes);
      return ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
    }
  }

  @Override
  public DataResult<IntStream> getIntStream(final BinaryTag input) {
    if (input instanceof final IntArrayBinaryTag intArrayBinaryTag) {
      return DataResult.success(intArrayBinaryTag.stream());
    } else {
      return DataResult.error(() -> "Expected an int array, got " + input.type());
    }
  }

  @Override
  public BinaryTag createIntList(final IntStream input) {
    return IntArrayBinaryTag.intArrayBinaryTag(input.toArray());
  }

  @Override
  public DataResult<LongStream> getLongStream(final BinaryTag input) {
    if (input instanceof final LongArrayBinaryTag longArrayBinaryTag) {
      return DataResult.success(longArrayBinaryTag.stream());
    } else {
      return DataResult.error(() -> "Expected a long array, got " + input.type());
    }
  }

  @Override
  public BinaryTag createLongList(final LongStream input) {
    return LongArrayBinaryTag.longArrayBinaryTag(input.toArray());
  }

  @Override
  public DataResult<BinaryTag> get(final BinaryTag input, final String key) {
    if (input instanceof final CompoundBinaryTag compoundBinaryTag) {
      final BinaryTag tag = compoundBinaryTag.get(key);
      if (tag != null) {
        return DataResult.success(tag);
      } else {
        return DataResult.error(() -> "Missing key '" + key + "' in map");
      }
    } else {
      return DataResult.error(() -> "Expected a compound tag, got " + input.type());
    }
  }

  @Override
  public DataResult<BinaryTag> getGeneric(final BinaryTag input, final BinaryTag key) {
    return unwrapStringResult(key).flatMap(stringKey -> this.get(input, stringKey));
  }

  @Override
  public BinaryTag set(final BinaryTag input, final String key, final BinaryTag value) {
    if (this.empty().equals(input)) {
      return CompoundBinaryTag.from(Map.of(key, value));
    } else if (input instanceof CompoundBinaryTag compoundBinaryTag) {
      return compoundBinaryTag.put(key, value);
    } else {
      return input;
    }
  }

  @Override
  public BinaryTag update(final BinaryTag input, final String key, final Function<BinaryTag, BinaryTag> function) {
    if (this.empty().equals(input)) {
      return input;
    } else if (input instanceof final CompoundBinaryTag compoundBinaryTag) {
      final BinaryTag old = compoundBinaryTag.get(key);
      if (old != null) {
        return compoundBinaryTag.put(key, function.apply(old));
      }
    }
    return input;
  }

  @Override
  public BinaryTag updateGeneric(final BinaryTag input, final BinaryTag key, final Function<BinaryTag, BinaryTag> function) {
    return this.update(input, unwrapString(key), function);
  }

  @Override
  public com.mojang.serialization.ListBuilder<BinaryTag> listBuilder() {
    return new BinaryTagListBuilder(this);
  }

  @Override
  public RecordBuilder<BinaryTag> mapBuilder() {
    return new CompoundBinaryTagRecordBuilder(this);
  }

  @Override
  public BinaryTag remove(final BinaryTag input, final String key) {
    if (input instanceof final CompoundBinaryTag compoundBinaryTag) {
      compoundBinaryTag.remove(key);
      return compoundBinaryTag;
    } else {
      return input;
    }
  }

  @Override
  public <U> U convertList(final DynamicOps<U> outOps, final BinaryTag input) {
    if (!(input instanceof ListBinaryTag listBinaryTag)) {
      throw new IllegalStateException("Expected a list, got " + input.type());
    }
    final com.mojang.serialization.ListBuilder<U> builder = outOps.listBuilder();
    for (final BinaryTag item : listBinaryTag) {
      builder.add(this.convertTo(outOps, item));
    }
    return builder.build(outOps.empty()).result().orElseThrow(() -> new IllegalArgumentException("Unable to convert tag"));
  }

  @Override
  public <U> U convertMap(final DynamicOps<U> outOps, final BinaryTag input) {
    if (!(input instanceof CompoundBinaryTag compoundBinaryTag)) {
      throw new IllegalStateException("Expected a compound, got " + input.type());
    }
    final RecordBuilder<U> builder = outOps.mapBuilder();
    for (final Map.Entry<String, ? extends BinaryTag> item : compoundBinaryTag) {
      builder.add(item.getKey(), this.convertTo(outOps, item.getValue()));
    }
    return builder.build(outOps.empty()).result().orElseThrow(() -> new IllegalArgumentException("Unable to convert tag"));
  }

  @Override
  public BinaryTag emptyMap() {
    return CompoundBinaryTag.empty();
  }

  @Override
  public BinaryTag emptyList() {
    return ListBinaryTag.empty();
  }

  @Override
  public BinaryTag createByte(final byte value) {
    return ByteBinaryTag.byteBinaryTag(value);
  }

  @Override
  public BinaryTag createShort(final short value) {
    return ShortBinaryTag.shortBinaryTag(value);
  }

  @Override
  public BinaryTag createInt(final int value) {
    return IntBinaryTag.intBinaryTag(value);
  }

  @Override
  public BinaryTag createLong(final long value) {
    return LongBinaryTag.longBinaryTag(value);
  }

  @Override
  public BinaryTag createFloat(final float value) {
    return FloatBinaryTag.floatBinaryTag(value);
  }

  @Override
  public BinaryTag createDouble(final double value) {
    return DoubleBinaryTag.doubleBinaryTag(value);
  }
}
