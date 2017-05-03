# lineTODO掲示板

TODOを登録、完了できるLinebotです。

困ったことを登録し、解決してもらうお願い登録掲示板としてシェアハウス内で利用しています。

使い方は下記
https://enigmatic-falls-51930.herokuapp.com/howto.html

## 始め方

1.LINE@ Accountを作成。Channel Access Tokenをコピーしておくこと。

2.使用するサーバーのドメイン(https必須)をWebhookURLに登録。(https://ドメイン名/json)

3.使用するサーバーのIPを、作成したLINE@ AccountのServerIP Whitelistに登録。

4.application.sample.confをapplication.confに変更。

5.application.confのACCESS_TOKENに1でコピーしたChannel Access Tokenを貼り付け。

6.application.confのdbにDBの設定を記述。

7.activator runを行い、http://localhost:9000にアクセスし、applyボタンをを選択してflywayのmigrateを実行