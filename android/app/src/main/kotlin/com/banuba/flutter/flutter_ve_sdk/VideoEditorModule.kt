package com.banuba.flutter.flutter_ve_sdk

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.banuba.sdk.arcloud.data.source.ArEffectsRepositoryProvider
import com.banuba.sdk.arcloud.di.ArCloudKoinModule
import com.banuba.sdk.audiobrowser.di.AudioBrowserKoinModule
import com.banuba.sdk.audiobrowser.domain.AudioBrowserMusicProvider
import com.banuba.sdk.cameraui.data.CameraConfig
import com.banuba.sdk.core.AspectRatio
import com.banuba.sdk.core.data.TrackData
import com.banuba.sdk.core.domain.AspectRatioProvider
import com.banuba.sdk.core.ui.ContentFeatureProvider
import com.banuba.sdk.effectplayer.adapter.BanubaEffectPlayerKoinModule
import com.banuba.sdk.export.di.VeExportKoinModule
import com.banuba.sdk.gallery.di.GalleryKoinModule
import com.banuba.sdk.playback.di.VePlaybackSdkKoinModule
import com.banuba.sdk.ve.data.EditorAspectSettings
import com.banuba.sdk.ve.data.aspect.AspectSettings
import com.banuba.sdk.ve.data.aspect.AspectsProvider
import com.banuba.sdk.ve.di.VeSdkKoinModule
import com.banuba.sdk.ve.flow.di.VeFlowKoinModule
import com.banuba.sdk.veui.data.EditorConfig
import com.banuba.sdk.veui.data.stickers.GifPickerConfigurations
import com.banuba.sdk.veui.di.VeUiSdkKoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

class VideoEditorModule {

    fun initialize(application: Application) {
        startKoin {
            androidContext(application)
            allowOverride(true)

            // IMPORTANT! order of modules is required
            modules(
                    VeSdkKoinModule().module,
                    VeExportKoinModule().module,
                    VePlaybackSdkKoinModule().module,

                    // Use AudioBrowserKoinModule ONLY if your contract includes this feature.
                    AudioBrowserKoinModule().module,

                    // IMPORTANT! ArCloudKoinModule should be set before TokenStorageKoinModule to
                    // get effects from the cloud
                    ArCloudKoinModule().module,
                    VeUiSdkKoinModule().module,
                    VeFlowKoinModule().module,
                    BanubaEffectPlayerKoinModule().module,
                    GalleryKoinModule().module,

                    // Sample integration module
                    SampleIntegrationVeKoinModule().module,
            )
        }
    }
}

/**
 * All dependencies mentioned in this module will override default implementations provided in VE UI
 * SDK. Some dependencies has no default implementations. It means that these classes fully depends
 * on your requirements
 */
private class SampleIntegrationVeKoinModule {

    val module = module {
        single<ArEffectsRepositoryProvider>(createdAtStart = true) {
            ArEffectsRepositoryProvider(
                    arEffectsRepository = get(named("backendArEffectsRepository")),
                    ioDispatcher = get(named("ioDispatcher"))
            )
        }
        // single configuration
        single {
            CameraConfig(
                    supportsGallery = true,
                    minRecordedTotalVideoDurationMs = 3000, // 2 mins for longform, 3 secs default
                    maxRecordedTotalVideoDurationMs =
                            15 * 60 * 1000, // 10 mins for longform, 2 mins default
                    videoDurations = listOf(3 * 60 * 1000L, 5 * 60 * 1000L, 10 * 60 * 1000L)
                    // Default durations
                    )
        }

        single {
            EditorConfig(
                    gallerySupportsImage = false,
                    gallerySupportsVideo = true,
                    minTotalVideoDurationMs = 3000, // 2 mins for longform, 3 secs default
                    maxTotalVideoDurationMs = 10 * 60 * 1000,
            )
        }
        single<AspectRatioProvider> {
            object : AspectRatioProvider {
                override fun provide(): AspectRatio {
                    return AspectRatio(16.0 / 9)
                }
            }
        }
        single<AspectsProvider> {
            object : AspectsProvider {
                private var bundle: Bundle? = null

                override var availableAspects: List<AspectSettings> =
                        listOf(EditorAspectSettings.`16_9`)

                override fun provide(): AspectsProvider.AspectsData {
                    return AspectsProvider.AspectsData(
                            allAspects = availableAspects,
                            default = availableAspects.first()
                    )
                }
                override fun setBundle(bundle: Bundle) {
                    this.bundle = bundle
                    // Check if we need to use the new editor UI/UX
                    val useEditorV2 = bundle.getBoolean("EXTRA_USE_EDITOR_V2", true)
                    // You can also update availableAspects based on bundle data if needed
                }
            }
        }
        factory<GifPickerConfigurations> { GifPickerConfigurations(giphyApiKey = "your GIPHY KEY") }

        // multiple configurations
        // single {
        //     CameraConfig(
        //             minRecordedTotalVideoDurationMs = 180_000L,
        //             maxRecordedTotalVideoDurationMs = 300_000L,
        //             videoDurations = listOf(300_000L, 180_000L)
        //     )
        // }

        // Audio Browser provider implementation.
        single<ContentFeatureProvider<TrackData, Fragment>>(named("musicTrackProvider")) {
            if (MainActivity.CONFIG_ENABLE_CUSTOM_AUDIO_BROWSER) {
                AudioBrowserContentProvider()
            } else {
                // Default implementation that supports Soundstripe, Mubert and Local audio stored
                // on the device
                AudioBrowserMusicProvider()
            }
        }
    }
}
