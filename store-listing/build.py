# Generates listing.md (short + full descriptions per language) and checks Play limits (short <= 80, full <= 4000).
L={}
def add(code,name,short,intro,notice,h1,b,h2,priv,src,web):
    L[code]=dict(name=name,short=short,intro=intro,notice=notice,h1=h1,b=b,h2=h2,priv=priv,src=src,web=web)
add('en-CA','English (Canada), default',
 "Unofficial TV app: videos, songs, karaoke, 2 subtitle languages. Free, no ads.",
 "Cedar TV is a free, open-source app for Google TV and Android TV that plays the public media library of jw.org with a player and interface designed for the TV remote.",
 "Cedar TV is an independent, unofficial app. It is not affiliated with, endorsed by or sponsored by jw.org or any organization.",
 "WHAT YOU CAN DO",
 ["Karaoke: sing along with highlighted lyrics on the big screen","Two subtitle languages at once, with timing adjustment and quick switching during the video","Over 300 languages for content, audio and subtitles, including right-to-left and sign languages","Continue watching, favorites, history and downloads that play offline","Search, music queue, sleep timer and a screensaver","Works from 720p to 4K with only the D-pad, OK, Back and Home"],
 "PRIVACY","No ads, no account, no tracking. Your settings, favorites and history stay on your TV.","Source code and support","Website")
add('uk','Ukrainian (uk)',
 "Неофіційний ТБ-застосунок: відео, пісні, караоке, субтитри 2 мовами.",
 "Cedar TV — безкоштовний застосунок із відкритим кодом для Google TV і Android TV, який відтворює загальнодоступну медіатеку jw.org із плеєром та інтерфейсом, створеними для пульта ТБ.",
 "Cedar TV — незалежний неофіційний застосунок. Він не пов’язаний із jw.org чи будь-якою організацією, не схвалений і не спонсорований ними.",
 "ЩО МОЖНА РОБИТИ",
 ["Караоке: співайте разом із підсвіченим текстом на великому екрані","Субтитри двома мовами одночасно, з підлаштуванням часу та швидким перемиканням під час відео","Понад 300 мов для вмісту, звуку й субтитрів, зокрема мови з письмом справа наліво та жестові мови","Продовження перегляду, вибране, історія та завантаження для перегляду офлайн","Пошук, черга музики, таймер сну та заставка","Працює від 720p до 4K, лише кнопками «вгору/вниз/вліво/вправо», OK, «Назад» і «Додому»"],
 "КОНФІДЕНЦІЙНІСТЬ","Без реклами, без облікового запису, без стеження. Ваші налаштування, вибране та історія залишаються на вашому телевізорі.","Вихідний код і підтримка","Вебсайт")
add('ru','Russian (ru)',
 "Неофициальное ТВ-приложение: видео, песни, караоке, субтитры на двух языках.",
 "Cedar TV — бесплатное приложение с открытым кодом для Google TV и Android TV, которое воспроизводит общедоступную медиатеку jw.org с плеером и интерфейсом, созданными для пульта ТВ.",
 "Cedar TV — независимое неофициальное приложение. Оно не связано с jw.org или какой-либо организацией, не одобрено и не спонсируется ими.",
 "ВОЗМОЖНОСТИ",
 ["Караоке: пойте вместе с подсвеченным текстом на большом экране","Субтитры на двух языках одновременно, с подстройкой времени и быстрым переключением во время видео","Более 300 языков для контента, звука и субтитров, включая письменность справа налево и жестовые языки","Продолжение просмотра, избранное, история и загрузки для просмотра офлайн","Поиск, очередь музыки, таймер сна и заставка","Работает от 720p до 4K, только кнопками «вверх/вниз/влево/вправо», OK, «Назад» и «Домой»"],
 "КОНФИДЕНЦИАЛЬНОСТЬ","Без рекламы, без аккаунта, без слежки. Ваши настройки, избранное и история остаются на вашем телевизоре.","Исходный код и поддержка","Сайт")
add('de','German (de-DE)',
 "Inoffizielle TV-App: Videos, Lieder, Karaoke, Untertitel in 2 Sprachen. Gratis.",
 "Cedar TV ist eine kostenlose Open-Source-App für Google TV und Android TV, die die öffentliche Mediathek von jw.org mit einem für die TV-Fernbedienung entwickelten Player und Bedienkonzept abspielt.",
 "Cedar TV ist eine unabhängige, inoffizielle App. Sie ist nicht mit jw.org oder einer Organisation verbunden und wird von keiner unterstützt, empfohlen oder gesponsert.",
 "DAS KANNST DU TUN",
 ["Karaoke: singe mit hervorgehobenem Liedtext auf dem großen Bildschirm mit","Untertitel in zwei Sprachen gleichzeitig, mit Zeitanpassung und schnellem Wechsel während des Videos","Über 300 Sprachen für Inhalte, Ton und Untertitel, auch Sprachen mit Schreibrichtung von rechts nach links und Gebärdensprachen","Weiterschauen, Favoriten, Verlauf und Downloads für die Wiedergabe ohne Internet","Suche, Musikwarteschlange, Einschlaftimer und ein Bildschirmschoner","Funktioniert von 720p bis 4K nur mit Steuerkreuz, OK, Zurück und Home"],
 "DATENSCHUTZ","Keine Werbung, kein Konto, kein Tracking. Deine Einstellungen, Favoriten und dein Verlauf bleiben auf deinem Fernseher.","Quellcode und Support","Website")
add('fr','French (fr-FR or fr-CA)',
 "Appli TV non officielle : vidéos, chants, karaoké, sous-titres en 2 langues.",
 "Cedar TV est une application gratuite et open source pour Google TV et Android TV qui lit la médiathèque publique de jw.org avec un lecteur et une interface conçus pour la télécommande.",
 "Cedar TV est une application indépendante et non officielle. Elle n’est ni affiliée à jw.org ou à une organisation, ni approuvée ou parrainée par eux.",
 "CE QUE VOUS POUVEZ FAIRE",
 ["Karaoké : chantez avec les paroles en surbrillance sur grand écran","Sous-titres en deux langues à la fois, avec réglage du décalage et changement rapide pendant la vidéo","Plus de 300 langues pour le contenu, l’audio et les sous-titres, y compris les langues s’écrivant de droite à gauche et les langues des signes","Reprise de lecture, favoris, historique et téléchargements à lire hors connexion","Recherche, file d’attente musicale, minuteur de mise en veille et économiseur d’écran","Fonctionne de 720p à 4K avec seulement les flèches, OK, Retour et Accueil"],
 "CONFIDENTIALITÉ","Aucune publicité, aucun compte, aucun suivi. Vos réglages, favoris et historique restent sur votre télé.","Code source et assistance","Site web")
add('es','Spanish (es-ES or es-419)',
 "App de TV no oficial: vídeos, canciones, karaoke y subtítulos en 2 idiomas.",
 "Cedar TV es una aplicación gratuita y de código abierto para Google TV y Android TV que reproduce la mediateca pública de jw.org con un reproductor y una interfaz diseñados para el mando de la tele.",
 "Cedar TV es una aplicación independiente y no oficial. No está afiliada a jw.org ni a ninguna organización, ni cuenta con su aprobación o patrocinio.",
 "QUÉ PUEDES HACER",
 ["Karaoke: canta con la letra resaltada en la pantalla grande","Subtítulos en dos idiomas a la vez, con ajuste de tiempo y cambio rápido durante el vídeo","Más de 300 idiomas para contenido, audio y subtítulos, incluidos idiomas de derecha a izquierda y lenguas de señas","Seguir viendo, favoritos, historial y descargas para ver sin conexión","Búsqueda, cola de música, temporizador de apagado y salvapantallas","Funciona de 720p a 4K solo con las flechas, OK, Atrás e Inicio"],
 "PRIVACIDAD","Sin anuncios, sin cuenta, sin rastreo. Tus ajustes, favoritos e historial se quedan en tu tele.","Código fuente y soporte","Sitio web")
add('it','Italian (it-IT)',
 "App TV non ufficiale: video, canzoni, karaoke, sottotitoli in 2 lingue. Gratis.",
 "Cedar TV è un’app gratuita e open source per Google TV e Android TV che riproduce la libreria multimediale pubblica di jw.org con un lettore e un’interfaccia pensati per il telecomando.",
 "Cedar TV è un’app indipendente e non ufficiale. Non è affiliata a jw.org né ad alcuna organizzazione, né da esse approvata o sponsorizzata.",
 "COSA PUOI FARE",
 ["Karaoke: canta con il testo evidenziato sul grande schermo","Sottotitoli in due lingue contemporaneamente, con regolazione della sincronia e cambio rapido durante il video","Oltre 300 lingue per contenuti, audio e sottotitoli, comprese lingue da destra a sinistra e lingue dei segni","Riprendi la visione, preferiti, cronologia e download da guardare offline","Ricerca, coda musicale, timer di spegnimento e salvaschermo","Funziona da 720p a 4K usando solo le frecce, OK, Indietro e Home"],
 "PRIVACY","Niente pubblicità, niente account, niente tracciamento. Impostazioni, preferiti e cronologia restano sulla tua TV.","Codice sorgente e assistenza","Sito web")
add('pt','Portuguese (pt-PT or pt-BR)',
 "App de TV não oficial: vídeos, músicas, karaoke e legendas em 2 idiomas.",
 "O Cedar TV é uma aplicação gratuita e de código aberto para Google TV e Android TV que reproduz a biblioteca multimédia pública de jw.org com um leitor e uma interface pensados para o comando da TV.",
 "O Cedar TV é uma aplicação independente e não oficial. Não é afiliada a jw.org nem a qualquer organização, nem por elas aprovada ou patrocinada.",
 "O QUE PODE FAZER",
 ["Karaoke: cante com a letra destacada no ecrã grande","Legendas em dois idiomas ao mesmo tempo, com ajuste de tempo e troca rápida durante o vídeo","Mais de 300 idiomas para conteúdo, áudio e legendas, incluindo idiomas da direita para a esquerda e línguas gestuais","Continuar a ver, favoritos, histórico e transferências para ver offline","Pesquisa, fila de música, temporizador de suspensão e proteção de ecrã","Funciona de 720p a 4K só com as setas, OK, Voltar e Início"],
 "PRIVACIDADE","Sem anúncios, sem conta, sem rastreamento. As suas definições, favoritos e histórico ficam na sua TV.","Código-fonte e suporte","Site")
add('nl','Dutch (nl-NL)',
 "Onofficiële tv-app: video’s, liederen, karaoke, ondertitels in 2 talen. Gratis.",
 "Cedar TV is een gratis open-source-app voor Google TV en Android TV die de openbare mediabibliotheek van jw.org afspeelt met een speler en interface die voor de tv-afstandsbediening zijn gemaakt.",
 "Cedar TV is een onafhankelijke, onofficiële app. Ze is niet verbonden aan jw.org of een andere organisatie en wordt niet door hen goedgekeurd of gesponsord.",
 "WAT JE KUNT DOEN",
 ["Karaoke: zing mee met gemarkeerde tekst op het grote scherm","Ondertitels in twee talen tegelijk, met tijdsaanpassing en snel wisselen tijdens de video","Meer dan 300 talen voor inhoud, audio en ondertitels, inclusief talen van rechts naar links en gebarentalen","Verder kijken, favorieten, geschiedenis en downloads om offline af te spelen","Zoeken, muziekwachtrij, slaaptimer en een screensaver","Werkt van 720p tot 4K met alleen de pijltjes, OK, Terug en Home"],
 "PRIVACY","Geen advertenties, geen account, geen tracking. Je instellingen, favorieten en geschiedenis blijven op je tv.","Broncode en support","Website")
add('pl','Polish (pl-PL)',
 "Nieoficjalna apka TV: filmy, pieśni, karaoke, napisy w 2 językach. Bez reklam.",
 "Cedar TV to bezpłatna aplikacja open source na Google TV i Android TV, która odtwarza publiczną bibliotekę multimediów jw.org w odtwarzaczu i interfejsie stworzonych z myślą o pilocie do telewizora.",
 "Cedar TV to niezależna, nieoficjalna aplikacja. Nie jest powiązana z jw.org ani żadną organizacją, nie jest przez nie zatwierdzona ani sponsorowana.",
 "CO MOŻESZ ROBIĆ",
 ["Karaoke: śpiewaj z podświetlanym tekstem na dużym ekranie","Napisy w dwóch językach naraz, z korektą czasu i szybkim przełączaniem podczas filmu","Ponad 300 języków treści, dźwięku i napisów, także pisanych od prawej do lewej i migowych","Kontynuacja oglądania, ulubione, historia i pobrane pliki do oglądania offline","Wyszukiwanie, kolejka muzyki, wyłącznik czasowy i wygaszacz ekranu","Działa od 720p do 4K wyłącznie za pomocą strzałek, OK, Wstecz i Ekranu głównego"],
 "PRYWATNOŚĆ","Bez reklam, bez konta, bez śledzenia. Ustawienia, ulubione i historia zostają na Twoim telewizorze.","Kod źródłowy i pomoc","Strona")
add('ro','Romanian (ro)',
 "Aplicație TV neoficială: video, cântări, karaoke, subtitrări în 2 limbi. Gratis.",
 "Cedar TV este o aplicație gratuită, open source, pentru Google TV și Android TV, care redă biblioteca media publică jw.org cu un player și o interfață făcute pentru telecomanda TV.",
 "Cedar TV este o aplicație independentă și neoficială. Nu este afiliată cu jw.org sau cu vreo organizație și nu este aprobată sau sponsorizată de acestea.",
 "CE POȚI FACE",
 ["Karaoke: cântă cu versurile evidențiate pe ecranul mare","Subtitrări în două limbi simultan, cu ajustarea sincronizării și comutare rapidă în timpul videoclipului","Peste 300 de limbi pentru conținut, audio și subtitrări, inclusiv limbi scrise de la dreapta la stânga și limbaje ale semnelor","Continuă vizionarea, favorite, istoric și descărcări pentru vizionare offline","Căutare, coadă de muzică, temporizator de oprire și economizor de ecran","Funcționează de la 720p la 4K doar cu săgețile, OK, Înapoi și Acasă"],
 "CONFIDENȚIALITATE","Fără reclame, fără cont, fără urmărire. Setările, favoritele și istoricul rămân pe televizorul tău.","Cod sursă și asistență","Site web")
add('ja','Japanese (ja-JP)',
 "非公式TVアプリ：動画、歌、カラオケ、2言語字幕。無料・広告なし。",
 "Cedar TV は、Google TV と Android TV 向けの無料のオープンソースアプリです。jw.org の公開メディアライブラリを、テレビのリモコン向けに作られたプレーヤーとインターフェースで再生します。",
 "Cedar TV は独立した非公式アプリです。jw.org やいかなる団体とも提携しておらず、承認も後援も受けていません。",
 "できること",
 ["カラオケ：大画面でハイライトされる歌詞に合わせて歌えます","2つの言語の字幕を同時に表示。タイミング調整や再生中のすばやい切り替えに対応","コンテンツ、音声、字幕に300以上の言語を用意。右から左に書く言語や手話にも対応","続きから再生、お気に入り、履歴、オフライン再生用のダウンロード","検索、音楽キュー、スリープタイマー、スクリーンセーバー","720pから4Kまで、十字キー、OK、戻る、ホームだけで操作できます"],
 "プライバシー","広告なし、アカウント不要、トラッキングなし。設定、お気に入り、履歴はテレビの中だけに保存されます。","ソースコードとサポート","ウェブサイト")
add('ko','Korean (ko-KR)',
 "비공식 TV 앱: 영상, 노래, 가라오케, 2개 언어 자막. 무료, 광고 없음.",
 "Cedar TV는 Google TV와 Android TV용 무료 오픈 소스 앱으로, TV 리모컨에 맞게 만든 플레이어와 인터페이스로 jw.org의 공개 미디어 라이브러리를 재생합니다.",
 "Cedar TV는 독립적인 비공식 앱입니다. jw.org 또는 어떤 단체와도 제휴, 승인, 후원 관계가 없습니다.",
 "이런 것을 할 수 있어요",
 ["가라오케: 큰 화면에서 강조 표시되는 가사와 함께 노래하세요","두 가지 언어 자막을 동시에 표시하고, 싱크 조절과 재생 중 빠른 전환 지원","콘텐츠, 음성, 자막에 300개 이상의 언어 지원(오른쪽에서 왼쪽으로 쓰는 언어와 수어 포함)","이어보기, 즐겨찾기, 시청 기록, 오프라인 재생용 다운로드","검색, 음악 대기열, 수면 타이머, 화면 보호기","720p부터 4K까지 방향키, 확인, 뒤로, 홈 버튼만으로 사용 가능"],
 "개인정보 보호","광고도 계정도 추적도 없습니다. 설정, 즐겨찾기, 시청 기록은 TV에만 저장됩니다.","소스 코드 및 지원","웹사이트")
add('zh','Chinese Simplified (zh-CN)',
 "非官方电视应用：视频、歌曲、卡拉OK、双语字幕。免费，无广告。",
 "Cedar TV 是一款适用于 Google TV 和 Android TV 的免费开源应用，用专为电视遥控器设计的播放器和界面播放 jw.org 的公开媒体库。",
 "Cedar TV 是独立的非官方应用，与 jw.org 或任何组织均无关联，也未获其认可或赞助。",
 "您可以做什么",
 ["卡拉OK：在大屏幕上跟着高亮歌词一起唱","同时显示两种语言的字幕，可调整时间，并能在播放中快速切换","内容、音频和字幕支持 300 多种语言，包括从右向左书写的语言和手语","继续观看、收藏、历史记录，以及可离线播放的下载","搜索、音乐队列、睡眠定时器和屏幕保护程序","从 720p 到 4K，只用方向键、确定、返回和主页键即可操作"],
 "隐私","无广告、无需账号、无跟踪。您的设置、收藏和历史记录只保存在您的电视上。","源代码与支持","网站")
add('ar','Arabic (ar)',
 "تطبيق تلفزيون غير رسمي: فيديوهات وترانيم وكاريوكي وترجمة بلغتين. مجاني.",
 "Cedar TV تطبيق مجاني ومفتوح المصدر لتلفزيونات Google TV وAndroid TV يعرض مكتبة الوسائط العامة لموقع jw.org بمشغّل وواجهة مصممين لريموت التلفزيون.",
 "Cedar TV تطبيق مستقل وغير رسمي. وهو غير تابع لموقع jw.org أو لأي منظمة، ولم تؤيده أو ترعه.",
 "ما يمكنك فعله",
 ["كاريوكي: غنِّ مع الكلمات المميّزة على الشاشة الكبيرة","ترجمة بلغتين في الوقت نفسه، مع ضبط التوقيت والتبديل السريع أثناء الفيديو","أكثر من 300 لغة للمحتوى والصوت والترجمة، بما فيها اللغات المكتوبة من اليمين إلى اليسار ولغات الإشارة","متابعة المشاهدة والمفضلة والسجل والتنزيلات للتشغيل دون اتصال","بحث وقائمة انتظار للموسيقى ومؤقّت نوم وشاشة توقف","يعمل من 720p إلى 4K بأزرار الأسهم وموافق ورجوع والرئيسية فقط"],
 "الخصوصية","بلا إعلانات ولا حساب ولا تتبع. تبقى إعداداتك ومفضلتك وسجلك على تلفزيونك.","الشيفرة المصدرية والدعم","الموقع")
out=["# Cedar TV: Google Play store listing text (all languages)\n",
"Generated by `build.py`. Paste Short description and Full description into Play Console > Store presence > Main store listing > Translations (Manage translations > add each language). Limits: short 80, full 4000. Machine-written by Claude, not reviewed by native speakers except where you read it.\n"]
bad=False
for c,d in L.items():
    full=d['intro']+"\n\n"+d['notice']+"\n\n"+d['h1']+"\n"+"\n".join("- "+b for b in d['b'])+"\n\n"+d['h2']+"\n"+d['priv']+"\n\n"+d['src']+": https://github.com/chapliak/cedar-tv\n"+d['web']+": https://chapliak.com/cedar-tv/"
    ls,lf=len(d['short']),len(full)
    flag=" **TOO LONG**" if ls>80 or lf>4000 else ""
    if flag: bad=True
    print(c,ls,lf,flag)
    out.append(f"\n## {d['name']}  (short {ls}/80, full {lf}/4000){flag}\n\n**App name:** Cedar TV\n\n**Short description**\n```\n{d['short']}\n```\n\n**Full description**\n```\n{full}\n```\n")
open('listing.md','w').write("\n".join(out))
print("BAD" if bad else "all within limits")
