import React from 'react'
import Bacon from 'baconjs'

import './HenkiloView.css'

import {l10nP} from '../../external/l10n'
import {henkiloNavi} from "../../external/navilists";
import {locationP} from "../../logic/location";

const HenkiloDuplicatesView = React.createClass({
    render: function() {
        const L = this.props.l10n;
        return (
            <div className="wrapper">
                <div className="header">
                    <h2>{L['HENKILO_PERUSTIEDOT_OTSIKKO']}</h2>
                </div>
                <p>
                    TODO: henkilön duplikaattien tietoja
                </p>
            </div>
        )
    }
});

export const henkiloDuplicatesViewContentP = Bacon.combineWith(l10nP, locationP, (l10n, location) => {
    const props = {l10n};
    henkiloNavi.oid = location.params['oid'];
    henkiloNavi.backLocation = '/henkilo';
    return {
        content: <HenkiloDuplicatesView {...props}/>,
        navi: henkiloNavi
    };
});

export default HenkiloDuplicatesView
