package io.openems.edge.pvinverter.sunspec;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;

public enum PvChannelId implements io.openems.edge.common.channel.ChannelId {
	PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
			.text("PV-Limit failed"));

	private final Doc doc;

	private PvChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}