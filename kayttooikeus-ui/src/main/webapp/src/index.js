import React from 'react'
import ReactDOM from 'react-dom'
import Bacon from 'baconjs'

import organisations from './organisations'

import VirkailijanLisaysLomake from './components/VirkailijanLisaysLomake/VirkailijanLisaysLomake'
import './index.css'

const appState = Bacon.combineTemplate({
  addedOrgs: organisations.toProperty([])
})

const mockData = {
  l10n: {
    "fi": {
      'VIRKAILIJAN_LISAYS_OTSIKKO': 'Virkalijan lisäys',
      'VIRKAILIJAN_TIEDOT_OTSIKKO': 'Virkalijan tiedot',
      'VIRKAILIJAN_TIEDOT_SPOSTI': 'Sähköposti',
      'VIRKAILIJAN_TIEDOT_KIELI': 'Kieli',
      'VIRKAILIJAN_LISAYS_ORGANISAATIOON_OTSIKKO': 'Virkalijan lisäys organisaatioon',
      'VIRKAILIJAN_LISAYS_ORGANISAATIOON_ORGANISAATIO': 'Organisaatio',
      'VIRKAILIJAN_LISAYS_ORGANISAATIOON_MYONNA_KAYTTOOIKEUKSIA': 'Myönnä käyttöoikeuksia',
      'VIRKAILIJAN_LISAYS_ORGANISAATIOON_LISAA': 'Lisää organisaatio...',
      'VIRKAILIJAN_LISAYS_TALLENNA': 'Tallenna ja lähetä itserekisteröitymissähköposti'
    }
  },
  languages: [
    { code: 'fi', "name-fi": 'suomi' },
    { code: 'sv', "name-fi": 'ruomi' },
    { code: 'en', "name-fi": 'englanti' }
  ],
  organisations: [
    { 
      id: 'org1', 
      "name-fi": 'Org 1', 
      permissions: [
        { id: 'org1perm1', "name-fi": 'Org1 Perm 1'},
        { id: 'org1perm2', "name-fi": 'Org1 Perm 2'},
        { id: 'org1perm3', "name-fi": 'Org1 Perm 3'},
      ]
    },
    { 
      id: 'org2', 
      "name-fi": 'Org 2',
      permissions: [
        { id: 'org2perm1', "name-fi": 'Org2 Perm 1'},
        { id: 'org2perm2', "name-fi": 'Org2 Perm 2'},
        { id: 'org2perm3', "name-fi": 'Org2 Perm 3'},
      ]
    },
    { 
      id: 'org3', 
      "name-fi": 'Org 3',
      permissions: [
        { id: 'org3perm1', "name-fi": 'Org3 Perm 1'},
        { id: 'org3perm2', "name-fi": 'Org3 Perm 2'},
        { id: 'org3perm3', "name-fi": 'Org3 Perm 3'},
      ]
    }
  ]
} 

appState.onValue(appState => {
  console.log(appState)
  ReactDOM.render(
    <VirkailijanLisaysLomake {...mockData} {...appState} />,
    document.getElementById('root')
  )
})


