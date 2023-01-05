<template>
  <div style="margin-left: 12%; margin-right: 12%; margin-top: 40px;">
    <div class="text-center text-primary text-h4" style="margin: 20px 35% 40px 30%;">
      $ 0
    </div>
    <q-form
      @submit="onSubmit"
      @reset="onReset"
      class="q-gutter-md"
    >
      <q-select
        use-chips
        stack-label
        label="To"
        use-input
        outlined
      />

      <q-input
        label="Amount"
        type="number"
        lazy-rules
        outlined
      />

      <q-input
        label="Note"
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
import { useQuasar } from 'quasar'
import {defineComponent, ref} from 'vue'

export default defineComponent({
  name: 'PayOrRequestForm',
  setup () {
    const $q = useQuasar()

    const name = ref(null)
    const age = ref(null)
    const accept = ref(false)

    return {
      name,
      age,
      accept,

      onSubmit () {
        if (accept.value !== true) {
          $q.notify({
            color: 'red-5',
            textColor: 'white',
            icon: 'warning',
            message: 'You need to accept the license and terms first'
          })
        }
        else {
          $q.notify({
            color: 'green-4',
            textColor: 'white',
            icon: 'cloud_done',
            message: 'Submitted'
          })
        }
      },

      onReset () {
        name.value = null
        age.value = null
        accept.value = false
      }
    }
  }
})
</script>
