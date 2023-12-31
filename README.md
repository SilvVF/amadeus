# amadeus
Amadeus is a Manga dex manga reader that supports offline reading and browsing of manga.
Features are based on [Tachiyomi](https://tachiyomi.org/) and [Neko](https://tachiyomi.org/forks/Neko/). 

# tech stack
- [Koin](https://insert-koin.io/)
- [Room](https://developer.android.com/jetpack/androidx/releases/room)
- [Voyager](https://voyager.adriel.cafe/)
- [Datastore](https://developer.android.com/jetpack/androidx/releases/datastore)
- [Sandwich](https://github.com/skydoves/sandwich)
- [Ktor Client](https://ktor.io/)
- [OkHttp](https://square.github.io/okhttp/)
- [Okio](https://square.github.io/okio/)
- [Compose](https://developer.android.com/jetpack/compose)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Tachiyomi Image Decoder](https://github.com/tachiyomiorg/image-decoder)
- [Coil](https://coil-kt.github.io/coil/)
- [Coroutines / Flow](https://kotlinlang.org/docs/coroutines-overview.html)
- [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- [kotlin serialization](https://kotlinlang.org/docs/serialization.html)

# modules

## core 
The core module contains the business logic for the app. 
Core is split into different parts that are used externally or by other core modules.

## sync
contains the logic for updating saved manga and chapters. Also responsible for updating tags and seasonal manga lists.

## feature
module for containing the different app feature submodules.



<img src="https://github.com/SilvVF/amadeus/assets/98186105/fe8a3c5f-6bdf-4a1e-8372-4d72ebac3f9a" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/11e93865-a300-44a3-8bb2-cdf7d4b81ae1" width='200'>
<img src=https://github.com/SilvVF/amadeus/assets/98186105/4d49452b-3ea2-43f0-b5fc-f382e80f7b19" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/89b942e8-f0d4-46ed-bd34-cbded5324479" width='200'>

![Screenshot_20231229_162656_amadeus.jpg](https://github.com/SilvVF/amadeus/assets/98186105/3722f753-7e97-4184-bffa-37ec1c4e8316)


