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

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;

final class CompoundBinaryTagRecordBuilder implements RecordBuilder<BinaryTag> {
  private final BinaryTagOps ops;
  private final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
  private Lifecycle life = Lifecycle.experimental();
  private DataResult.@Nullable Error<BinaryTag> error = null;

  CompoundBinaryTagRecordBuilder(final BinaryTagOps ops) {
    this.ops = ops;
  }

  @Override
  public DynamicOps<BinaryTag> ops() {
    return this.ops;
  }

  @Override
  public RecordBuilder<BinaryTag> add(final BinaryTag key, final BinaryTag value) {
    this.builder.put(BinaryTagOpsImpl.unwrapString(key), value);
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> add(final BinaryTag key, final DataResult<BinaryTag> value) {
    if (value.error().isPresent()) {
      this.error = value.error().get();
    } else {
      this.add(key, value.result().orElseThrow(() -> new IllegalStateException("Neither error or result was present")));
    }
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> add(final DataResult<BinaryTag> key, final DataResult<BinaryTag> value) {
    if (key.error().isPresent()) {
      this.error = key.error().get();
    } else {
      this.add(key.result().orElseThrow(() -> new IllegalStateException("Neither error or result was present")), value);
    }
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> withErrorsFrom(final DataResult<?> result) {
    if (result.error().isPresent()) {
      this.error = result.error().get().map(x -> this.ops.empty());
    }
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> setLifecycle(final Lifecycle lifecycle) {
    this.life = lifecycle;
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> mapError(final UnaryOperator<String> onError) {
    if (this.error != null) {
      final Supplier<String> oldMessage = this.error.messageSupplier();
      this.error = new DataResult.Error<>(() -> onError.apply(oldMessage.get()), Optional.empty(), this.life);
    }
    return this;
  }

  @Override
  public DataResult<BinaryTag> build(final BinaryTag prefix) {
    if (this.error != null) {
      return DataResult.error(this.error.messageSupplier(), this.life);
    } else {
      return DataResult.success(this.builder.build(), this.life);
    }
  }
}
