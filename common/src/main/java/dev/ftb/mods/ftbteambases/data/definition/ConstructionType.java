package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ConstructionType(Optional<PrebuiltStructure> prebuilt, Optional<Pregen> pregen, Optional<JigsawParams> jigsaw) {
    public static final Codec<ConstructionType> CODEC = ExtraCodecs.xor(PrebuiltStructure.CODEC, ExtraCodecs.xor(Pregen.CODEC, JigsawParams.CODEC))
            .xmap(ConstructionType::merge, ConstructionType::split);

    private static ConstructionType merge(Either<PrebuiltStructure, Either<Pregen, JigsawParams>> either) {
        return either.map(ConstructionType::ofPrebuilt, other -> other.map(ConstructionType::ofPregen, ConstructionType::ofJigsaw));
    }

    @NotNull
    private static Either<PrebuiltStructure, Either<Pregen, JigsawParams>> split(ConstructionType type) {
        if (type.prebuilt.isPresent()) return Either.left(type.prebuilt.get());
        if (type.pregen.isPresent()) return Either.right(Either.left(type.pregen.get()));
        if (type.jigsaw.isPresent()) return Either.right(Either.right(type.jigsaw.get()));
        throw new IllegalStateException("none of prebuilt/pregen/jigsaw are present!");
    }

    public static ConstructionType ofPrebuilt(PrebuiltStructure prebuiltStructure) {
        return new ConstructionType(Optional.of(prebuiltStructure), Optional.empty(), Optional.empty());
    }

    public static ConstructionType ofPregen(Pregen pregen) {
        return new ConstructionType(Optional.empty(), Optional.of(pregen), Optional.empty());
    }

    public static ConstructionType ofJigsaw(JigsawParams jigsawParams) {
        return new ConstructionType(Optional.empty(), Optional.empty(), Optional.of(jigsawParams));
    }

    public static ConstructionType fromBytes(FriendlyByteBuf buf) {
        return switch (buf.readEnum(TypeID.class)) {
            case PREBUILT -> ConstructionType.ofPrebuilt(PrebuiltStructure.fromBytes(buf));
            case PREGEN -> ConstructionType.ofPregen(Pregen.fromBytes(buf));
            case JIGSAW -> ConstructionType.ofJigsaw(JigsawParams.fromBytes(buf));
        };
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (prebuilt.isPresent()) {
            buf.writeEnum(TypeID.PREBUILT);
            prebuilt.get().toBytes(buf);
        } else if (pregen.isPresent()) {
            buf.writeEnum(TypeID.PREGEN);
            pregen.get().toBytes(buf);
        } else if (jigsaw.isPresent()) {
            buf.writeEnum(TypeID.JIGSAW);
            jigsaw.get().toBytes(buf);
        } else {
            throw new IllegalStateException("none of prebuilt/pregen/jigsaw present!");
        }
    }

    enum TypeID {
        PREBUILT,
        PREGEN,
        JIGSAW
    }
}
