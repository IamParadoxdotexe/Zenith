package dev.shadowsoffire.apotheosis.adventure.affix.augmenting;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.placebo.reload.DynamicHolder;

public record RerollResultMessage(DynamicHolder<? extends Affix> newAffix) { //todo reroll result message

/*    public static class Provider implements MessageProvider<RerollResultMessage> {

        @Override
        public Class<?> getMsgClass() {
            return RerollResultMessage.class;
        }

        @Override
        public void write(RerollResultMessage msg, FriendlyByteBuf buf) {
            buf.writeResourceLocation(msg.newAffix.getId());
        }

        @Override
        public RerollResultMessage read(FriendlyByteBuf buf) {
            return new RerollResultMessage(AffixRegistry.INSTANCE.holder(buf.readResourceLocation()));
        }

        @Override
        public void handle(RerollResultMessage msg, Supplier<Context> ctx) {
            MessageHelper.handlePacket(() -> {
                AugmentingScreen.handleRerollResult(msg.newAffix());
            }, ctx);
        }

    }*/

}