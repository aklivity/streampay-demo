import {boot} from 'quasar/wrappers'
import { domain, clientId as client_id } from '../../auth_config.json';
import { createAuth0 } from '@auth0/auth0-vue';

// "async" is optional;
// more info on params: https://v2.quasar.dev/quasar-cli/boot-files
export default boot( ({ app}) => {
  app.use(
    createAuth0({
      domain,
      client_id,
      redirect_uri: `${window.location.origin}`,
      audience: 'http://localhost:8080/chat',
      scope: 'read:users write:users read:channels write:channels read:subscriptions write:subscriptions'
    })
  )
})
