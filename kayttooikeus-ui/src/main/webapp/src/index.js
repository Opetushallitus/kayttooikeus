import React from 'react'
import ReactDOM from 'react-dom'
import Bacon from 'baconjs'
// enable polyfill when ready eject from create-react-app (https://github.com/github/fetch)
// import fetch from 'whatwg-fetch'

import organisations from './logic/organisations'
import basicInfo from './logic/basicInfo'
import l10nResponseS, { l10nResponsePendingP } from './external/l10n'
import orgsResponseS, { orgsResponsePendingP } from './external/organisations'
import langResponseS, { langResponsePendingP } from './external/languages'
import InvitationForm from './components/InvitationForm'

import './reset.css'
import './index.css'

const appStateS = Bacon.combineTemplate({
  addedOrgs: organisations.toProperty(),
  basicInfo: basicInfo.toProperty(),
  l10n: l10nResponseS.toProperty(),
  orgs: orgsResponseS.toProperty(),
  languages: langResponseS.toProperty(),
}).changes()

appStateS
  .skipWhile(orgsResponsePendingP
    .or(l10nResponsePendingP)
    .or(langResponsePendingP))
  .onValue(appState => {
    ReactDOM.render(
      <InvitationForm {...appState} />, document.getElementById('root')
    )
  })
