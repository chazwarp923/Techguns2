package techguns;

import java.util.function.Supplier;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techguns.api.capabilities.AttackTime;
import techguns.capabilities.TGExtendedPlayer;

/** Data attachments, replacing the 1.12.2 capabilities. */
public class TGAttachments {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Techguns.MODID);

	public static final Supplier<AttachmentType<TGExtendedPlayer>> EXTENDED_PLAYER = ATTACHMENTS.register("extended_player",
			() -> AttachmentType.serializable(TGExtendedPlayer::new).copyOnDeath().build());

	/** Recoil/reload/muzzle flash timing of non-player shooters (was TGShooterValues capability); not saved. */
	public static final Supplier<AttachmentType<AttackTime>> SHOOTER_VALUES = ATTACHMENTS.register("shooter_values",
			() -> AttachmentType.builder(AttackTime::new).build());
}
