package kr.ac.hansung.smartrent.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 판정 작업·공휴일 받기 등 스케줄러를 켜 둠. 작업은 각 이슈에서 만듭니다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
