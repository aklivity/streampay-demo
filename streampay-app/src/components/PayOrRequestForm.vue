<template>
  <div style="margin-left: 12%; margin-right: 12%; margin-top: 40px;">
    <div class="text-center text-primary text-h4" style="margin: 20px 35% 40px 30%;">
      $ {{ balance }}
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
        v-model="user"
        :options="userOptions"
      />

      <q-input
        label="Amount"
        type="number"
        v-model="amount"
        lazy-rules
        outlined
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
import {defineComponent, ref} from 'vue'
import {api} from "boot/axios";
import {useQuasar} from "quasar";
import {useRouter} from "vue-router";
import {v4} from "uuid";

export default defineComponent({
  name: 'PayOrRequestForm',
  setup () {
    const $q = useQuasar()
    const balance = ref(0);
    const router = useRouter();
    api.get('/balances/user1')
      .then((response) => {
        balance.value = response.data;
      })
      .catch(() => {
        balance.value = 0;
      });

    const user = ref(null);
    const userOptions = ref([]);
    api.get('/users')
      .then((response) => {
        const users = response.data;
        for(let user of users) {
          if (user.id != 'user1') {
            userOptions.value.push(
              {
                label: user.name,
                value: user.id
              }
            );
          }
        }
      });
    const amount = ref(0);
    const notes = ref("");

    return {
      balance,
      user,
      userOptions,
      amount,
      notes,
      onPay () {
        if (balance.value - amount.value > 0) {
          api.post('/pay', {
            userId: user.value.value,
            amount: amount.value,
            notes: notes.value
          },{
            headers: {
              'Idempotency-Key': v4()
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
      onRequest () {
        api.post('/request', {
          userId: user.value.value,
          amount: amount.value,
          notes: notes.value
        },{
          headers: {
            'Idempotency-Key': v4()
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
  }
})
</script>
