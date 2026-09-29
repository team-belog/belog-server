package org.com.belog.postlog.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PostLogPhotoVerificationProperties::class)
class PostLogPhotoVerificationConfig
