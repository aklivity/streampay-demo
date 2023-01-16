<template>
  <div style="margin-left: 12%; margin-right: 12%; margin-top: 40px;">
    <div class="text-center text-primary text-h4" style="margin: 20px 35% 40px 30%;">
      ${{ balance }}
    </div>
    <q-form
      @submit="onPay"
      @reset="onRequest"
      class="q-gutter-md"
    >
      <q-select
        use-chips
        stack-label
        label="To"
        use-input
        outlined
        v-model="userOption"
        :options="userOptions"
        :rules="[
           (val) =>
              (val && val.value.length > 0) ||
              'Please select user',
        ]"
      />

      <q-input
        label="Amount"
        type="number"
        v-model="amount"
        lazy-rules
        outlined
        :rules="[
          (val) =>
            (val && val > 0) || 'Required field and should be more than $0.',
        ]"
      />

      <q-input
        v-model="notes"
        label="Notes"
        type="textarea"
        outlined
      />

      <div style="margin-left: 15%; margin-bottom: 20px;  margin-top: 20px;">
        <q-btn label="Pay" style="width: 200px" type="submit" color="primary" rounded />
        <q-btn label="Request" style="width: 200px" type="reset" color="primary" class="q-ml-sm" rounded />
      </div>
    </q-form>

  </div>
</template>

<script>
import {defineComponent, ref, toRefs} from 'vue'
import {api} from "boot/axios";
import {useQuasar} from "quasar";
import {useRouter} from "vue-router";
import {v4} from "uuid";
import {useAuth0} from "@auth0/auth0-vue";

export default defineComponent({
  name: 'PayOrRequestForm',
  props: {
    requestId: {
      type: String
    }
  },
  setup (props) {
    const $q = useQuasar()
    const auth0 = useAuth0();
    const { requestId } = toRefs(props);

    const balance = ref(0);
    const userOption = ref(null);
    const userOptions = ref([]);
    const amount = ref(0);
    const notes = ref("");
    const router = useRouter();

    return {
      auth0,
      user: auth0.user,
      formRequestId: requestId,
      balance,
      userOption,
      userOptions,
      amount,
      notes,
      async onPay () {
        if (balance.value - amount.value > 0) {
          const accessToken = await auth0.getAccessTokenSilently();
          const authorization = { Authorization: `Bearer ${accessToken}` };
          api.post('/pay', {
            userId: userOption.value.value,
            amount: amount.value,
            notes: notes.value,
            requestId: requestId.value
          },{
            headers: {
              'Idempotency-Key': v4(),
              ...authorization
            }}).then(function () {
            router.push({ path: '/main' });
          })
            .catch(function (error) {
              $q.notify({
                position: 'top',
                color: 'red-5',
                textColor: 'white',
                icon: 'error',
                message: error
              });
            });
        } else {
          $q.notify({
            position: 'top',
            color: 'red-5',
            textColor: 'white',
            icon: 'error',
            message: "You don't have enough balance."
          });
        }
      },
      async onRequest () {
        const accessToken = await auth0.getAccessTokenSilently();
        const authorization = { Authorization: `Bearer ${accessToken}` };
        api.post('/request', {
          userId: userOption.value.value,
          amount: amount.value,
          notes: notes.value
        },{
          headers: {
            'Idempotency-Key': v4(),
            ...authorization
        }}).then(function () {
          router.push({ path: '/main' });
        })
        .catch(function (error) {
          $q.notify({
            position: 'top',
            color: 'red-5',
            textColor: 'white',
            icon: 'error',
            message: error
          });
        });
      }
    }
  },
  async mounted() {
    const accessToken = await this.auth0.getAccessTokenSilently();
    const authorization = { Authorization: `Bearer ${accessToken}` };

    if (this.formRequestId) {
      api.get('/payment-requests/' + this.formRequestId,{
          headers: {
            ...authorization
          }
        })
        .then((response) => {
          const request = response.data;
          this.amount.value = request.amount;

          api.get('/users/' + this.user.sub)
            .then((response) => {
              const user = response.data;

              const newUserOption = {
                label: user.name,
                value: user.id
              };
              this.userOption.value = newUserOption;
              this.userOptions.value.push(newUserOption);
            });
        })
    } else {
      api.get('/users', {
          headers: {
            ...authorization
          }
        })
        .then((response) => {
          const users = response.data;
          for(let user of users) {
            if (user.id != this.user.sub) {
              const newUserOption = {
                label: user.name,
                value: user.id
              };
              this.userOptions.push(newUserOption);
            }
          }
        });
    }
  }
})
</script>
