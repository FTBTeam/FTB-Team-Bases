package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ConstructionType(Optional<PrebuiltStructure> prebuilt, Optional<Pregen> pregen, Optional<JigsawParams> jigsaw, Optional<SingleStructure> singleStructure) {
    public static final Codec<ConstructionType> CODEC
            = ExtraCodecs.xor(PrebuiltStructure.CODEC, ExtraCodecs.xor(Pregen.CODEC, ExtraCodecs.xor(JigsawParams.CODEC, SingleStructure.CODEC)))
            .xmap(ConstructionType::merge, ConstructionType::split);

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

    public static ConstructionType fromBytes(FriendlyByteBuf buf) {
        return switch (buf.readEnum(TypeID.class)) {
            case PREBUILT -> ConstructionType.ofPrebuilt(PrebuiltStructure.fromBytes(buf));
            case PREGEN -> ConstructionType.ofPregen(Pregen.fromBytes(buf));
            case JIGSAW -> ConstructionType.ofJigsaw(JigsawParams.fromBytes(buf));
            case SINGLE -> ConstructionType.ofSingleStructure(SingleStructure.fromBytes(buf));
        };
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (prebuilt.isPresent()) {
            TypeID.PREBUILT.write(prebuilt.get(), buf);
        } else if (pregen.isPresent()) {
            TypeID.PREGEN.write(pregen.get(), buf);
        } else if (jigsaw.isPresent()) {
            TypeID.JIGSAW.write(jigsaw.get(), buf);
        } else if (singleStructure.isPresent()) {
            TypeID.SINGLE.write(singleStructure.get(), buf);
        } else {
            throw new IllegalStateException("none of prebuilt/pregen/jigsaw/single-structure present!");
        }
    }

    enum TypeID {
        PREBUILT,
        PREGEN,
        JIGSAW,
        SINGLE;

        public void write(INetworkWritable obj, FriendlyByteBuf buf) {
            buf.writeEnum(this);
            obj.toBytes(buf);
        }
    }
}
