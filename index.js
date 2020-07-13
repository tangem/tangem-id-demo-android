//integration of jsonld.js normalization
const {LiquidCore} = require('liquidcore')
const jsonld = require('jsonld')

LiquidCore.on( 'normalize', (json) => {
	jsonld.normalize(json).then((normalized) => LiquidCore.emit( 'normalized', normalized ));
})

LiquidCore.emit( 'ready' );