# amadeus 
[GooglePlay](https://play.google.com/store/apps/details?id=io.silv.amadeusreader)

Amadeus is a Manga dex manga reader that supports offline reading and browsing of manga.
Features are based on [Tachiyomi](https://tachiyomi.org/) and [Neko](https://tachiyomi.org/forks/Neko/). 
Rip tachiyomi [o7]("https://www.youtube.com/watch?v=EAk8PjCsXQ8")

# tech stack
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

## Gallery

<img src="https://github.com/SilvVF/amadeus/assets/98186105/76a84abc-c7c4-4848-82ad-67514863031c" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/1cab0d54-5f98-4286-9acc-ebd838a0bcc1" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/4106fb39-a123-406d-bdb0-1e68ee6d23dc" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/18c67894-678e-463c-aac2-403a880901ed" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/144d0933-5fc0-4b01-b57e-ab22cb5c9f0b" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/f335bb8b-0cfe-4fa6-a591-3bcb970ee8ad" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/f561cdf8-804a-4a60-b266-74495e43b2a5" width='200'>
<img src="https://github.com/SilvVF/amadeus/assets/98186105/5e3f4315-0bfe-487b-82ac-b7087135fc71" width='200'>



