package kr.ac.hansung.smartrent.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import kr.ac.hansung.smartrent.global.config.TimeConfig;

/** 시험에서 시각을 옮길 수 있는 Clock(예: 로그인 잠금 5분 뒤) */
public class MutableClock extends Clock {

	private Instant instant;

	public MutableClock(Instant start) {
		this.instant = start;
	}

	public void advance(Duration d) {
		instant = instant.plus(d);
	}

	public void set(Instant at) {
		instant = at;
	}

	@Override
	public ZoneId getZone() {
		return TimeConfig.ZONE;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return instant;
	}
}
