package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ConstructionType(Optional<PrebuiltStructure> prebuilt, Optional<Pregen> pregen, Optional<JigsawParams> jigsaw, Optional<SingleStructure> singleStructure) {
    public static final Codec<ConstructionType> CODEC
            = Codec.xor(PrebuiltStructure.CODEC, Codec.xor(Pregen.CODEC, Codec.xor(JigsawParams.CODEC, SingleStructure.CODEC)))
            .xmap(ConstructionType::merge, ConstructionType::split);
    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructionType> STREAM_CODEC = StreamCodec.of(
            (buf, object) -> {
                if (object.prebuilt.isPresent()) {
                    TypeID.PREBUILT.write(object.prebuilt.get(), buf);
                } else if (object.pregen.isPresent()) {
                    TypeID.PREGEN.write(object.pregen.get(), buf);
                } else if (object.jigsaw.isPresent()) {
                    TypeID.JIGSAW.write(object.jigsaw.get(), buf);
                } else if (object.singleStructure.isPresent()) {
                    TypeID.SINGLE.write(object.singleStructure.get(), buf);
                } else {
                    throw new IllegalStateException("none of prebuilt/pregen/jigsaw/single-structure present!");
                }
            },
            buf -> switch (buf.readEnum(TypeID.class)) {
                case PREBUILT -> ConstructionType.ofPrebuilt(PrebuiltStructure.STREAM_CODEC.decode(buf));
                case PREGEN -> ConstructionType.ofPregen(Pregen.STREAM_CODEC.decode(buf));
                case JIGSAW -> ConstructionType.ofJigsaw(JigsawParams.STREAM_CODEC.decode(buf));
                case SINGLE -> ConstructionType.ofSingleStructure(SingleStructure.STREAM_CODEC.decode(buf));
            }
    );

    private static ConstructionType merge(Either<PrebuiltStructure, Either<Pregen, Either<JigsawParams, SingleStructure>>> either) {
        return either.map(ConstructionType::ofPrebuilt,
                other -> other.map(ConstructionType::ofPregen,
                        other2 -> other2.map(ConstructionType::ofJigsaw, ConstructionType::ofSingleStructure)
                )
        );
    }

    @NotNull
    private static Either<PrebuiltStructure, Either<Pregen, Either<JigsawParams, SingleStructure>>> split(ConstructionType type) {
        if (type.prebuilt.isPresent()) return Either.left(type.prebuilt.get());
        if (type.pregen.isPresent()) return Either.right(Either.left(type.pregen.get()));
        if (type.jigsaw.isPresent()) return Either.right(Either.right(Either.left(type.jigsaw.get())));
        if (type.singleStructure.isPresent()) return Either.right(Either.right(Either.right(type.singleStructure.get())));

        throw new IllegalStateException("none of prebuilt/pregen/jigsaw/single-structure are present!");
    }

    public static ConstructionType ofPrebuilt(PrebuiltStructure prebuiltStructure) {
        return new ConstructionType(Optional.of(prebuiltStructure), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ConstructionType ofPregen(Pregen pregen) {
        return new ConstructionType(Optional.empty(), Optional.of(pregen), Optional.empty(), Optional.empty());
    }

    public static ConstructionType ofJigsaw(JigsawParams jigsawParams) {
        return new ConstructionType(Optional.empty(), Optional.empty(), Optional.of(jigsawParams), Optional.empty());
    }

    public static ConstructionType ofSingleStructure(SingleStructure singleStructure) {
        return new ConstructionType(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(singleStructure));
    }

    enum TypeID {
        PREBUILT,
        PREGEN,
        JIGSAW,
        SINGLE;

        public <T extends INetworkWritable<T>> void write(T obj, RegistryFriendlyByteBuf buf) {
            buf.writeEnum(this);
            obj.streamCodec().encode(buf, obj);
        }
    }
}
