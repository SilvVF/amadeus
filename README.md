# amadeus
Amadeus is a Manga dex manga reader that supports offline reading and browsing of manga.

## Tech Stack
- [Koin](https://insert-koin.io/)
- [Ktor Client](https://ktor.io/)
- [Coil](https://coil-kt.github.io/coil/compose/)
- [KotlinxSerialization](https://kotlinlang.org/docs/serialization.html)
- [Coroutines + Flow](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [voyager](https://voyager.adriel.cafe/)
- [Paging3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)

The app also uses a modified version of [Sandwhich](https://github.com/skydoves/sandwich) which is changed to support using ktor client instead of retrofit.
This can be found in the [ktor-response-mapper](https://github.com/SilvVF/amadeus/tree/master/ktor-response-mapper) module.

## Modules
The app is split into four modules.
- The app modules relies on all of the modules and contains the Ui specific jetpack compose code.
- The manga module manages all the data related to repsonses from the manga dex api and saving this data.
- The core module contains any Util funcitons that can be used througout the app that are not android specific.
- The ktor-response-mapper module transforms ktor responses based on [Sandwhich](https://github.com/skydoves/sandwich)

## Data Flow
The app follows Unidirectional Data Flow Pattern (UDF) and uses best practices recommended by Android docs.

1. At the launch of the app WorkManager is used to sync the saved manga and look for any updates to chapters.
This happens in the [App](https://github.com/SilvVF/amadeus/blob/master/app/src/main/java/io/silv/amadeus/AmadeusApp.kt) Sync.init(ctx)
tags for manga dex filters are also update here.
3. The [MangaSyncWorker](https://github.com/SilvVF/amadeus/blob/master/manga/src/main/java/io/silv/manga/local/workers/MangaSyncWorker.kt) will update the saved manga and chapters      using the Syncable repositorys sync method.
  The [TagSyncWorker](https://github.com/SilvVF/amadeus/blob/master/manga/src/main/java/io/silv/manga/local/workers/TagSyncWorker.kt) will update filterable tags.
  The [SeasonalMangaSyncWorker](https://github.com/SilvVF/amadeus/blob/master/manga/src/main/java/io/silv/manga/local/workers/SeasonalMangaSyncWorker.kt) will update seasonal lists     from the mangadex admin list.
4. All changes from the above sync workers will be updated in the Local database and the Ui of the app will be able to observe these changes

## Saving images
When saving images if the manga is from a thirdparty uploader on mangadex it is not gaurenteed that the images will show correctly. All images that come from the mangadex api are 
supported.

## App images
<img src="https://github.com/SilvVF/amadeus/assets/98186105/f45a0efd-58de-4ed3-b482-3353201afb4b" width=300>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/cb687834-a69f-42fe-a1df-6921c0d2767e" width=300>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/3adffca4-fe15-4e7a-bfb3-a46c5ffb35fc" width=300>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/a859ad00-e6c8-4ef6-8128-447dc6abb242" width=300>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/7789c840-b449-4a4b-9fcf-1aa24d6f9d74" width=300>
